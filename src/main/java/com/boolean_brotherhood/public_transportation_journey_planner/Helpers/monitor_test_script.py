#!/usr/bin/env python3
"""Comprehensive monitoring suite for the Public Transportation Journey Planner."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import sys
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

import requests
from requests import RequestException


@dataclass
class EndpointSpec:
    """Describes a single endpoint invocation."""

    name: str
    path: str
    method: str = "GET"
    expected_status: int = 200
    params: Optional[Dict[str, Any]] = None
    json: Optional[Dict[str, Any]] = None


class TransportSystemMonitor:
    """Runs the monitoring suite against a single base URL."""

    def __init__(
        self,
        base_url: str,
        timeout: float = 10.0,
        label: Optional[str] = None,
        retries: int = 0,
        concurrent_threads: int = 5,
        concurrent_iterations: int = 3,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.label = label or self.base_url
        self.timeout = timeout
        self.retries = max(0, retries)
        self.concurrent_threads = max(1, concurrent_threads)
        self.concurrent_iterations = max(1, concurrent_iterations)

        self.session = requests.Session()
        self.session.headers.update({"User-Agent": "PTJP-Monitor/2.0"})

        self.log_lines: List[Dict[str, Any]] = []
        self.endpoint_results: List[Dict[str, Any]] = []
        self.context: Dict[str, Any] = {}
        self.lock = threading.Lock()

    # ------------------------------------------------------------------
    # Logging helpers
    # ------------------------------------------------------------------
    def log(self, message: str, level: str = "INFO") -> None:
        entry = {
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "level": level,
            "message": message,
        }
        with self.lock:
            self.log_lines.append(entry)
        print(f"[{entry['timestamp']}] {level}: {message}")

    def _record_endpoint_result(self, result: Dict[str, Any]) -> None:
        with self.lock:
            self.endpoint_results.append(result)

    # ------------------------------------------------------------------
    # Context bootstrap
    # ------------------------------------------------------------------
    def bootstrap_context(self) -> None:
        self.log("Preparing context data for dynamic endpoint checks ...")
        self.context.clear()

        graph_stops = self._fetch_json("/api/graph/stops")
        if isinstance(graph_stops, list) and graph_stops:
            self.context["graph_stop"] = graph_stops[0].get("name")
            if len(graph_stops) > 1:
                self.context["graph_stop_alt"] = graph_stops[1].get("name")
            coords = (
                graph_stops[0].get("latitude"),
                graph_stops[0].get("longitude"),
            )
            if all(isinstance(c, (int, float)) for c in coords):
                self.context["graph_coords"] = coords

        self.context["train_stop"] = self._first_name(self._fetch_json("/api/train/stops"))
        self.context["ga_stop"] = self._first_name(self._fetch_json("/api/GA/stops"))
        self.context["ga_stop_alt"] = self._second_name(self._fetch_json("/api/GA/stops"))
        self.context["myciti_stop"] = self._first_name(self._fetch_json("/api/myciti/stops"))
        self.context["myciti_stop_alt"] = self._second_name(self._fetch_json("/api/myciti/stops"))
        taxi_stops = self._fetch_json("/api/taxi/all-stops")
        if isinstance(taxi_stops, list) and taxi_stops:
            self.context["taxi_coords"] = (
                taxi_stops[0].get("latitude"),
                taxi_stops[0].get("longitude"),
            )

        self.log(f"Context prepared: {json.dumps(self.context, default=str)}")

    def _fetch_json(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Optional[Any]:
        url = f"{self.base_url}{endpoint}"
        try:
            response = self.session.get(url, params=params, timeout=self.timeout)
            if response.status_code != 200:
                return None
            return response.json()
        except (RequestException, ValueError):
            return None

    @staticmethod
    def _first_name(items: Optional[List[Dict[str, Any]]]) -> Optional[str]:
        if isinstance(items, list) and items:
            return items[0].get("name")
        return None

    @staticmethod
    def _second_name(items: Optional[List[Dict[str, Any]]]) -> Optional[str]:
        if isinstance(items, list) and len(items) > 1:
            return items[1].get("name")
        return None

    # ------------------------------------------------------------------
    # Endpoint execution
    # ------------------------------------------------------------------
    def test_endpoint(self, spec: EndpointSpec, log_request: bool = True) -> Dict[str, Any]:
        url = f"{self.base_url}{spec.path}"
        attempt = 0
        last_exception: Optional[Exception] = None

        while attempt <= self.retries:
            start_time = time.time()
            try:
                if spec.method.upper() == "POST":
                    response = self.session.post(
                        url,
                        json=spec.json,
                        params=spec.params,
                        timeout=self.timeout,
                    )
                else:
                    response = self.session.get(
                        url,
                        params=spec.params,
                        timeout=self.timeout,
                    )

                duration_ms = (time.time() - start_time) * 1000
                success = response.status_code == spec.expected_status
                payload: Any
                try:
                    payload = response.json()
                except ValueError:
                    payload = response.text[:500]

                result: Dict[str, Any] = {
                    "base_url": self.base_url,
                    "name": spec.name,
                    "endpoint": spec.path,
                    "status_code": response.status_code,
                    "response_time_ms": round(duration_ms, 2),
                    "success": success,
                    "data": payload,
                    "timestamp": datetime.now().isoformat(),
                }

                self._record_endpoint_result(result)
                if log_request:
                    if success:
                        self.log(f"? {spec.name} ({response.status_code}) in {result['response_time_ms']} ms")
                    else:
                        self.log(
                            f"? {spec.name} expected {spec.expected_status} got {response.status_code}",
                            level="WARN",
                        )
                return result

            except RequestException as exc:
                duration_ms = (time.time() - start_time) * 1000
                last_exception = exc
                result = {
                    "base_url": self.base_url,
                    "name": spec.name,
                    "endpoint": spec.path,
                    "status_code": 0,
                    "response_time_ms": round(duration_ms, 2),
                    "success": False,
                    "error": str(exc),
                    "timestamp": datetime.now().isoformat(),
                }
                self._record_endpoint_result(result)
                if log_request:
                    self.log(f"? {spec.name} request error: {exc}", level="ERROR")
            attempt += 1

        if last_exception is not None:
            raise last_exception
        return result  # type: ignore[UnboundLocalVariable]

    # ------------------------------------------------------------------
    # Test plan definition
    # ------------------------------------------------------------------
    def build_test_plan(self) -> List[tuple[str, List[EndpointSpec]]]:
        plan: List[tuple[str, List[EndpointSpec]]] = []

        # Graph controller
        graph_specs: List[EndpointSpec] = [
            EndpointSpec("Graph metrics", "/api/graph/metrics"),
            EndpointSpec("Graph stops", "/api/graph/stops"),
        ]
        graph_stop = self.context.get("graph_stop")
        graph_stop_alt = self.context.get("graph_stop_alt")
        coords = self.context.get("graph_coords")
        if graph_stop:
            graph_specs.append(EndpointSpec("Graph stop detail", f"/api/graph/stops/{graph_stop}"))
        if coords and all(isinstance(c, (int, float)) for c in coords):
            graph_specs.append(
                EndpointSpec(
                    "Graph nearest stop",
                    "/api/graph/stops/nearest",
                    params={"lat": coords[0], "lon": coords[1]},
                )
            )
        if graph_stop and graph_stop_alt:
            graph_specs.append(
                EndpointSpec(
                    "Graph journey",
                    "/api/graph/journey",
                    params={
                        "from": graph_stop,
                        "to": graph_stop_alt,
                        "time": "07:30",
                        "day": "WEEKDAY",
                    },
                )
            )
        plan.append(("Graph", graph_specs))

        # GA bus controller
        ga_specs: List[EndpointSpec] = [
            EndpointSpec("GA metrics", "/api/GA/metrics"),
            EndpointSpec("GA stops", "/api/GA/stops"),
            EndpointSpec("GA trips", "/api/GA/trips"),
        ]
        ga_stop = self.context.get("ga_stop")
        ga_stop_alt = self.context.get("ga_stop_alt")
        if ga_stop and ga_stop_alt:
            ga_specs.append(
                EndpointSpec(
                    "GA journey",
                    "/api/GA/journey",
                    params={
                        "source": ga_stop,
                        "target": ga_stop_alt,
                        "departure": "07:45",
                        "maxRounds": 4,
                    },
                )
            )
        plan.append(("GA Bus", ga_specs))

        # MyCiti controller
        myciti_specs: List[EndpointSpec] = [
            EndpointSpec("MyCiti metrics", "/api/myciti/metrics"),
            EndpointSpec("MyCiti stops", "/api/myciti/stops"),
            EndpointSpec("MyCiti trips", "/api/myciti/trips"),
        ]
        myciti_stop = self.context.get("myciti_stop")
        myciti_stop_alt = self.context.get("myciti_stop_alt")
        if myciti_stop and myciti_stop_alt:
            myciti_specs.append(
                EndpointSpec(
                    "MyCiti journey",
                    "/api/myciti/journey",
                    params={
                        "source": myciti_stop,
                        "target": myciti_stop_alt,
                        "departure": "08:00",
                        "maxRounds": 4,
                    },
                )
            )
        plan.append(("MyCiti Bus", myciti_specs))

        # Train controller
        train_specs: List[EndpointSpec] = [
            EndpointSpec("Train metrics", "/api/train/metrics"),
            EndpointSpec("Train stops", "/api/train/stops"),
        ]
        train_stop = self.context.get("train_stop")
        if train_stop:
            train_specs.append(EndpointSpec("Train stop detail", f"/api/train/stops/{train_stop}"))
            train_specs.append(
                EndpointSpec(
                    "Train journey",
                    "/api/train/journey",
                    params={"from": train_stop, "to": train_stop, "time": "06:30"},
                )
            )
        plan.append(("Train", train_specs))

        # Taxi controller
        taxi_specs: List[EndpointSpec] = [
            EndpointSpec("Taxi metrics", "/api/taxi/metrics"),
            EndpointSpec("Taxi all stops", "/api/taxi/all-stops"),
            EndpointSpec("Taxi all trips", "/api/taxi/all-trips"),
        ]
        taxi_coords = self.context.get("taxi_coords") or self.context.get("graph_coords")
        if taxi_coords and all(isinstance(c, (int, float)) for c in taxi_coords):
            taxi_specs.append(
                EndpointSpec(
                    "Taxi nearest stops",
                    "/api/taxi/nearest-stops",
                    method="POST",
                    json={
                        "location": {"latitude": taxi_coords[0], "longitude": taxi_coords[1]},
                        "max": 5,
                    },
                )
            )
        plan.append(("Taxi", taxi_specs))

        # Admin controller
        admin_specs = [
            EndpointSpec("Admin system metrics", "/api/admin/systemMetrics"),
            EndpointSpec("Admin files in use", "/api/admin/GetFileInUse"),
            EndpointSpec("Admin recent calls", "/api/admin/MostRecentCall"),
        ]
        plan.append(("Admin", admin_specs))

        # Monitoring controller
        monitor_specs = [
            EndpointSpec("Monitor health", "/api/monitor/health", expected_status=200),
            EndpointSpec("Monitor summary", "/api/monitor/summary"),
            EndpointSpec("Monitor ready", "/api/monitor/ready", expected_status=200),
            EndpointSpec("Monitor stats", "/api/monitor/stats"),
            EndpointSpec("Monitor alerts", "/api/monitor/alerts"),
            EndpointSpec("Monitor alerts all", "/api/monitor/alerts/all"),
            EndpointSpec("Monitor performance", "/api/monitor/performance"),
            EndpointSpec("Monitor health check", "/api/monitor/health/check", method="POST"),
        ]
        for graph_name in ("train", "bus", "taxi"):
            monitor_specs.append(
                EndpointSpec(
                    f"Monitor graph {graph_name} ready",
                    f"/api/monitor/graph/{graph_name}/ready",
                    expected_status=200,
                )
            )
            monitor_specs.append(
                EndpointSpec(
                    f"Monitor graph {graph_name} status",
                    f"/api/monitor/graph/{graph_name}",
                    expected_status=200,
                )
            )
        plan.append(("Monitor", monitor_specs))

        return plan

    # ------------------------------------------------------------------
    # Performance tests
    # ------------------------------------------------------------------
    def run_performance_checks(self) -> None:
        targets = [
            EndpointSpec("Performance snapshot", "/api/monitor/performance"),
            EndpointSpec("Graph metrics snapshot", "/api/graph/metrics"),
            EndpointSpec("Admin system metrics snapshot", "/api/admin/systemMetrics"),
        ]
        self.log(
            f"Running performance probes with {self.concurrent_threads} threads x {self.concurrent_iterations} iterations"
        )

        def workload(spec: EndpointSpec) -> None:
            for _ in range(self.concurrent_iterations):
                try:
                    self.test_endpoint(spec, log_request=False)
                except RequestException as exc:
                    self.log(f"Performance probe error for {spec.name}: {exc}", level="WARN")

        with concurrent.futures.ThreadPoolExecutor(max_workers=self.concurrent_threads) as executor:
            for spec in targets:
                executor.submit(workload, spec)

    # ------------------------------------------------------------------
    # Orchestration
    # ------------------------------------------------------------------
    def run_full_test_suite(self) -> Dict[str, Any]:
        started = datetime.now()
        self.log(f"Starting monitoring for {self.label} at {started.strftime('%Y-%m-%d %H:%M:%S')}")

        try:
            self.bootstrap_context()
            for group_name, specs in self.build_test_plan():
                self.log(f"--- Testing {group_name} endpoints ---")
                for spec in specs:
                    try:
                        self.test_endpoint(spec)
                    except RequestException as exc:
                        self.log(f"Failed to reach {spec.name}: {exc}", level="ERROR")
            self.run_performance_checks()
            summary = self._build_summary()
        except Exception as exc:  # noqa: BLE001
            self.log(f"Unexpected error during monitoring: {exc}", level="ERROR")
            summary = {"error": str(exc)}

        finished = datetime.now()
        self.log(f"Monitoring completed at {finished.strftime('%Y-%m-%d %H:%M:%S')}")

        return {
            "base_url": self.base_url,
            "label": self.label,
            "started": started.isoformat(),
            "finished": finished.isoformat(),
            "summary": summary,
            "logs": list(self.log_lines),
            "endpoints": list(self.endpoint_results),
        }

    def _build_summary(self) -> Dict[str, Any]:
        total = len(self.endpoint_results)
        success = sum(1 for item in self.endpoint_results if item.get("success"))
        average_latency = (
            sum(item.get("response_time_ms", 0.0) for item in self.endpoint_results) / total
            if total
            else 0.0
        )
        return {
            "totalEndpoints": total,
            "successful": success,
            "failed": total - success,
            "successRate": round((success / total) * 100, 2) if total else 0.0,
            "averageLatencyMs": round(average_latency, 2),
        }


# ----------------------------------------------------------------------
# CLI orchestration
# ----------------------------------------------------------------------

def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the Public Transportation Journey Planner monitoring suite",
    )
    parser.add_argument(
        "--target",
        dest="targets",
        action="append",
        help="Base URL to test (can be provided multiple times).",
    )
    parser.add_argument(
        "--label",
        dest="labels",
        action="append",
        help="Optional label for the matching --target (order sensitive).",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        help="Request timeout in seconds (default: 10).",
    )
    parser.add_argument(
        "--retries",
        type=int,
        default=0,
        help="Number of retries per endpoint on failure (default: 0).",
    )
    parser.add_argument(
        "--concurrent-threads",
        type=int,
        default=5,
        help="Threads used in the concurrent monitoring test (default: 5).",
    )
    parser.add_argument(
        "--concurrent-iterations",
        type=int,
        default=3,
        help="Requests per thread in the concurrent monitoring test (default: 3).",
    )
    parser.add_argument(
        "--export",
        type=Path,
        help="Optional path to export a JSON report.",
    )
    parser.add_argument(
        "--fail-threshold",
        type=float,
        default=80.0,
        help="Minimum success rate percentage required for zero exit code (default: 80).",
    )
    return parser.parse_args(argv)


def run_monitors(args: argparse.Namespace) -> List[Dict[str, Any]]:
    targets = args.targets or ["http://localhost:8080"]
    labels = args.labels or []
    results: List[Dict[str, Any]] = []

    for index, target in enumerate(targets):
        label = labels[index] if index < len(labels) else None
        monitor = TransportSystemMonitor(
            base_url=target,
            timeout=args.timeout,
            label=label,
            retries=args.retries,
            concurrent_threads=args.concurrent_threads,
            concurrent_iterations=args.concurrent_iterations,
        )
        results.append(monitor.run_full_test_suite())
        print("\n" + "-" * 80)
    return results


def export_report(results: List[Dict[str, Any]], export_path: Path) -> None:
    export_path.parent.mkdir(parents=True, exist_ok=True)
    with export_path.open("w", encoding="utf-8") as handle:
        json.dump(results, handle, indent=2, default=str)
    print(f"\n? Report exported to: {export_path}")


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    results = run_monitors(args)

    if args.export:
        export_report(results, args.export)

    if not results:
        print("\n? Monitoring produced no results.")
        return 1

    worst_rate = min(result.get("summary", {}).get("successRate", 0.0) for result in results)
    if worst_rate < args.fail_threshold:
        print(
            f"\n? Lowest success rate {worst_rate:.1f}% is below threshold {args.fail_threshold:.1f}%"
        )
        return 1

    print("\n? Monitoring completed within acceptable thresholds.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
