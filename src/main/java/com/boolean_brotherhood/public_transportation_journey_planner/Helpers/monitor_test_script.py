#!/usr/bin/env python3

"""
Transport System Monitor Test Script (deployment-ready)

This script comprehensively tests the monitoring capabilities of the
Public Transportation Journey Planner system. It supports testing one or
more base URLs (local or remote) and can export structured JSON
summaries, making it suitable for deployment validation pipelines.
"""

import argparse
import concurrent.futures
import json
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

import requests
from requests import RequestException
import threading


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
        self.base_url = base_url.rstrip('/')
        self.label = label or self.base_url
        self.timeout = timeout
        self.retries = max(0, retries)
        self.concurrent_threads = max(1, concurrent_threads)
        self.concurrent_iterations = max(1, concurrent_iterations)

        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "PTJP-Monitor/1.0",
        })

        self.log_lines: List[Dict[str, Any]] = []
        self.endpoint_results: List[Dict[str, Any]] = []
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
    # Core HTTP testing helper
    # ------------------------------------------------------------------
    def test_endpoint(
        self,
        endpoint: str,
        expected_status: int = 200,
        method: str = "GET",
        payload: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        url = f"{self.base_url}{endpoint}"
        attempt = 0

        while True:
            start_time = time.time()
            try:
                if method.upper() == "POST":
                    response = self.session.post(url, json=payload, timeout=self.timeout)
                else:
                    response = self.session.get(url, timeout=self.timeout)

                response_time = (time.time() - start_time) * 1000
                result: Dict[str, Any] = {
                    "base_url": self.base_url,
                    "endpoint": endpoint,
                    "status_code": response.status_code,
                    "response_time_ms": round(response_time, 2),
                    "success": response.status_code == expected_status,
                    "timestamp": datetime.now().isoformat(),
                }

                if response.status_code == expected_status:
                    try:
                        data = response.json()
                        result["data"] = data
                        result["has_data"] = bool(data)
                    except ValueError:
                        result["data"] = response.text[:500]
                        result["has_data"] = bool(response.text)
                else:
                    result["error"] = response.text[:500]

            except RequestException as exc:
                response_time = (time.time() - start_time) * 1000
                result = {
                    "base_url": self.base_url,
                    "endpoint": endpoint,
                    "status_code": 0,
                    "response_time_ms": round(response_time, 2),
                    "success": False,
                    "error": str(exc),
                    "timestamp": datetime.now().isoformat(),
                }

            self._record_endpoint_result(result)

            if result["success"] or attempt >= self.retries:
                return result

            attempt += 1
            self.log(f"Retrying {endpoint} (attempt {attempt + 1}/{self.retries + 1})", "WARN")
            time.sleep(0.5)

    # ------------------------------------------------------------------
    # Test categories (adapted from original script)
    # ------------------------------------------------------------------
    def test_health_monitoring(self) -> None:
        self.log("=" * 60)
        self.log("TESTING HEALTH MONITORING ENDPOINTS")
        self.log("=" * 60)

        health_endpoints = [
            "/api/monitor/health",
            "/api/monitor/summary",
            "/api/monitor/ready",
            "/api/monitor/alerts",
            "/api/monitor/alerts/all",
            "/api/monitor/stats",
        ]

        for endpoint in health_endpoints:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"? {endpoint}: {result['response_time_ms']}ms")
                if endpoint == "/api/monitor/health" and "data" in result:
                    data = result["data"]
                    self.log(
                        f"   System Status: {'HEALTHY' if data.get('systemHealthy') else 'UNHEALTHY'}"
                    )
                    self.log(f"   Health Checks: {data.get('totalHealthChecks', 'N/A')}")
                    self.log(f"   Critical Errors: {data.get('criticalErrors', 'N/A')}")
            else:
                self.log(f"? {endpoint}: {result.get('error', 'Failed')}", "ERROR")

    def test_graph_monitoring(self) -> None:
        self.log("\n" + "=" * 60)
        self.log("TESTING GRAPH-SPECIFIC MONITORING")
        self.log("=" * 60)

        for graph in ["train", "bus", "taxi"]:
            self.log(f"\nTesting {graph.upper()} Graph:")

            ready_result = self.test_endpoint(f"/api/monitor/graph/{graph}/ready")
            if ready_result["success"] and "data" in ready_result:
                ready = ready_result["data"].get("ready", False)
                self.log(f"  Ready Status: {'? READY' if ready else '? NOT READY'}")

            status_result = self.test_endpoint(f"/api/monitor/graph/{graph}")
            if status_result["success"] and "data" in status_result:
                status = status_result["data"]
                self.log(f"  Status: {status.get('status', 'UNKNOWN')}")
                self.log(f"  Stops: {status.get('stopCount', 'N/A')}")
                self.log(f"  Trips: {status.get('tripCount', 'N/A')}")
                self.log(f"  Load Time: {status.get('loadTimeMs', 'N/A')}ms")

    def test_admin_monitoring(self) -> None:
        self.log("\n" + "=" * 60)
        self.log("TESTING ADMIN MONITORING ENDPOINTS")
        self.log("=" * 60)

        admin_endpoints = [
            "/api/admin/systemMetrics",
            "/api/admin/GetFileInUse",
            "/api/admin/MostRecentCall",
        ]

        for endpoint in admin_endpoints:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"? {endpoint}: {result['response_time_ms']}ms")
                if "data" in result and isinstance(result["data"], (dict, list)):
                    size = len(result["data"]) if isinstance(result["data"], list) else len(result["data"].keys())
                    self.log(f"   Data elements: {size}")
            else:
                self.log(f"? {endpoint}: {result.get('error', 'Failed')}", "ERROR")

    def test_service_endpoints(self) -> None:
        self.log("\n" + "=" * 60)
        self.log("TESTING MAIN SERVICE ENDPOINTS")
        self.log("=" * 60)

        endpoints = [
            ("/api/train/stops", "Train stops"),
            ("/api/train/metrics", "Train metrics"),
            ("/api/myciti/stops", "MyCiti stops"),
            ("/api/myciti/metrics", "MyCiti metrics"),
            ("/api/taxi/all-stops", "Taxi stops"),
            ("/api/taxi/metrics", "Taxi metrics"),
        ]

        for endpoint, label in endpoints:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"? {label}: {result['response_time_ms']}ms")
                if isinstance(result.get("data"), list):
                    self.log(f"   Records: {len(result['data'])}")
            else:
                self.log(f"? {label}: {result.get('error', 'Failed')}", "ERROR")

    def test_concurrent_monitoring(self) -> None:
        self.log("\n" + "=" * 60)
        self.log("TESTING CONCURRENT MONITORING LOAD")
        self.log("=" * 60)

        endpoints = [
            "/api/monitor/health",
            "/api/monitor/ready",
            "/api/admin/systemMetrics",
            "/api/train/metrics",
            "/api/myciti/metrics",
            "/api/taxi/metrics",
        ]

        def worker() -> List[Dict[str, Any]]:
            worker_results = []
            for endpoint in endpoints:
                for _ in range(self.concurrent_iterations):
                    worker_results.append(self.test_endpoint(endpoint))
                    time.sleep(0.1)
            return worker_results

        self.log(
            f"Starting {self.concurrent_threads} threads, {self.concurrent_iterations} iterations each..."
        )
        start_time = time.time()

        all_results: List[Dict[str, Any]] = []
        with concurrent.futures.ThreadPoolExecutor(max_workers=self.concurrent_threads) as executor:
            futures = [executor.submit(worker) for _ in range(self.concurrent_threads)]
            for future in concurrent.futures.as_completed(futures):
                try:
                    all_results.extend(future.result())
                except Exception as exc:
                    self.log(f"Thread failed: {exc}", "ERROR")

        total_time = time.time() - start_time
        success_count = sum(1 for r in all_results if r["success"])
        total_requests = len(all_results)
        success_rate = (success_count / total_requests * 100) if total_requests else 0.0

        self.log(f"Concurrent test completed in {total_time:.2f}s")
        self.log(f"Success rate: {success_count}/{total_requests} ({success_rate:.1f}%)")
        if total_requests:
            avg_time = sum(r["response_time_ms"] for r in all_results) / total_requests
            self.log(f"Average response time: {avg_time:.2f}ms")

    def test_forced_health_check(self) -> None:
        self.log("\n" + "=" * 60)
        self.log("TESTING FORCED HEALTH CHECK")
        self.log("=" * 60)

        result = self.test_endpoint("/api/monitor/health/check", method="POST")
        if result["success"]:
            data = result.get("data", {})
            self.log(f"? Forced health check: {result['response_time_ms']}ms")
            self.log(f"   System Status: {data.get('systemStatus', 'Unknown')}")
            self.log(f"   Active Alerts: {len(data.get('activeAlerts', []))}")
        else:
            self.log(f"? Forced health check failed: {result.get('error', 'Failed')}", "ERROR")

    def test_journey_endpoints(self) -> None:
        self.log("\n" + "=" * 60)
        self.log("TESTING JOURNEY PLANNING (MONITORING IMPACT)")
        self.log("=" * 60)

        journeys = [
            ("/api/train/journey?from=Cape Town&to=Bellville&time=08:00", "Train journey"),
            ("/api/myciti/journey?source=Civic Centre&target=Airport&departure=09:00", "MyCiti journey"),
            ("/api/taxi/all-stops", "Taxi stops lookup"),
        ]

        for endpoint, label in journeys:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"? {label}: {result['response_time_ms']}ms")
            else:
                self.log(f"? {label}: {result.get('error', 'Failed')}", "WARN")

        time.sleep(1)
        recent_calls = self.test_endpoint("/api/admin/MostRecentCall")
        if recent_calls["success"] and "data" in recent_calls:
            self.log(f"Recent endpoint calls logged: {len(recent_calls['data'])}")

    # ------------------------------------------------------------------
    # Reporting
    # ------------------------------------------------------------------
    def generate_report(self) -> Dict[str, Any]:
        total = len(self.endpoint_results)
        success = sum(1 for r in self.endpoint_results if r["success"])
        failure = total - success
        avg_time = (
            round(sum(r["response_time_ms"] for r in self.endpoint_results) / total, 2)
            if total
            else 0.0
        )
        success_rate = (success / total * 100) if total else 0.0

        rating = "CRITICAL ISSUES"
        if success_rate >= 90:
            rating = "EXCELLENT"
        elif success_rate >= 75:
            rating = "GOOD"
        elif success_rate >= 50:
            rating = "NEEDS ATTENTION"

        self.log("\n" + "=" * 60)
        self.log("MONITORING TEST SUMMARY REPORT")
        self.log("=" * 60)
        self.log(f"Total Tests: {total}")
        self.log(f"Successful: {success}")
        self.log(f"Failed: {failure}")
        self.log(f"Success Rate: {success_rate:.1f}%")
        self.log(f"Average response time: {avg_time:.2f}ms")
        self.log(f"Status: {rating}")

        summary = {
            "total": total,
            "success": success,
            "failure": failure,
            "successRate": round(success_rate, 2),
            "averageResponseTimeMs": avg_time,
            "rating": rating,
        }
        return summary

    def run_full_test_suite(self) -> Dict[str, Any]:
        started = datetime.now()
        self.log("?? STARTING COMPREHENSIVE MONITORING TESTS")
        self.log(f"Target System: {self.base_url}")
        self.log(f"Test Start Time: {started.strftime('%Y-%m-%d %H:%M:%S')}")

        summary: Dict[str, Any]
        try:
            self.test_health_monitoring()
            self.test_graph_monitoring()
            self.test_admin_monitoring()
            self.test_service_endpoints()
            self.test_forced_health_check()
            self.test_journey_endpoints()
            self.test_concurrent_monitoring()
            summary = self.generate_report()
        except KeyboardInterrupt:
            self.log("\n? Testing interrupted by user", "WARN")
            summary = {"interrupted": True}
        except Exception as exc:
            self.log(f"\n?? Unexpected error during testing: {exc}", "ERROR")
            summary = {"error": str(exc)}

        finished = datetime.now()
        self.log(f"\n? Testing completed at {finished.strftime('%Y-%m-%d %H:%M:%S')}")

        return {
            "base_url": self.base_url,
            "label": self.label,
            "started": started.isoformat(),
            "finished": finished.isoformat(),
            "summary": summary,
            "logs": list(self.log_lines),
            "endpoints": list(self.endpoint_results),
        }


# ----------------------------------------------------------------------
# CLI orchestration
# ----------------------------------------------------------------------

def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the Public Transportation Journey Planner monitoring suite."
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
        default=50.0,
        help="Minimum success rate percentage required for zero exit code (default: 50).",
    )
    return parser.parse_args(argv)


def run_monitors(args: argparse.Namespace) -> List[Dict[str, Any]]:
    targets = args.targets or ["https://pjtp-brotherhood.up.railway.app"]
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
    print(f"\n?? Report exported to: {export_path}")


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    results = run_monitors(args)

    if args.export:
        export_report(results, args.export)

    worst_rate = min(
        result.get("summary", {}).get("successRate", 0.0)
        for result in results
    ) if results else 0.0

    if worst_rate < args.fail_threshold:
        print(
            f"\n??  Lowest success rate {worst_rate:.1f}% is below threshold {args.fail_threshold:.1f}%"
        )
        return 1

    print("\n? Monitoring completed within acceptable thresholds.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
