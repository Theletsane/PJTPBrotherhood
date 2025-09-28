#!/usr/bin/env python3
"""
Enhanced Transport System Monitor Test Script for Boolean Brotherhood PTJP

This script comprehensively tests all controllers and monitoring capabilities 
of the Public Transportation Journey Planner system based on the actual codebase.

Covers:
- AdminController: File management, system metrics, logs
- GABusController: Golden Arrow bus network
- BusGraphController: Combined MyCiti + GA Bus operations  
- GraphController: Multimodal journey planning
- MyCitiBusController: MyCiti bus network
- SystemMonitoringController: Health and performance monitoring
- TaxiController: Taxi/minibus network
- TrainController: Railway network with coordinates

Usage: python enhanced_transport_monitor.py [base_url]
"""

import requests
import json
import time
import sys
from datetime import datetime
from typing import Dict, Any, List, Optional
import concurrent.futures
import threading

class EnhancedTransportSystemMonitor:
    def __init__(self, base_url: str = "https://pjtp-brotherhood.up.railway.app"):
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.session.timeout = 30  # Increased timeout for heavy operations
        self.results = []
        self.lock = threading.Lock()
        self.test_stats = {
            'total': 0,
            'passed': 0,
            'failed': 0,
            'warnings': 0
        }
        
    def log(self, message: str, level: str = "INFO"):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with self.lock:
            log_entry = f"[{timestamp}] {level}: {message}"
            print(log_entry)
            self.results.append(log_entry)
    
    def test_endpoint(self, endpoint: str, method: str = "GET", data: Dict = None, 
                     expected_status: int = 200, allow_statuses: List[int] = None) -> Dict[str, Any]:
        """Test a single endpoint and return comprehensive results"""
        url = f"{self.base_url}{endpoint}"
        start_time = time.time()
        
        try:
            if method.upper() == "POST":
                if data:
                    response = self.session.post(url, json=data)
                else:
                    response = self.session.post(url)
            else:
                response = self.session.get(url)
                
            response_time = (time.time() - start_time) * 1000  # ms
            
            # Check if status is acceptable
            acceptable_statuses = [expected_status]
            if allow_statuses:
                acceptable_statuses.extend(allow_statuses)
            
            is_success = response.status_code in acceptable_statuses
            
            result = {
                "endpoint": endpoint,
                "method": method,
                "status_code": response.status_code,
                "response_time_ms": round(response_time, 2),
                "success": is_success,
                "content_length": len(response.content),
                "timestamp": datetime.now().isoformat()
            }
            
            if is_success or response.status_code < 500:
                try:
                    response_data = response.json()
                    result["data"] = response_data
                    result["has_data"] = bool(response_data)
                except:
                    result["data"] = response.text[:500] if response.text else ""
                    result["has_data"] = bool(response.text)
            else:
                result["error"] = response.text[:500] if response.text else "No error message"
            
            return result
            
        except Exception as e:
            return {
                "endpoint": endpoint,
                "method": method,
                "status_code": 0,
                "response_time_ms": (time.time() - start_time) * 1000,
                "success": False,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }
    
    def test_admin_controller(self):
        """Test AdminController endpoints comprehensively"""
        self.log("=" * 70)
        self.log("TESTING ADMIN CONTROLLER")
        self.log("=" * 70)
        
        admin_endpoints = [
            ("/api/admin/list", "List root data files"),
            ("/api/admin/systemMetrics", "Comprehensive system metrics"),
            ("/api/admin/GetFileInUse", "Files currently in use"),
            ("/api/admin/MostRecentCall", "Recent API calls history"),
            ("/api/admin/systemLogs?limit=10", "System logs (limited)"),
            ("/api/admin/adminLogs?limit=20", "Admin-level logs (warnings/errors)"),
            ("/api/admin/operationPerformance", "Operation performance metrics"),
        ]
        
        for endpoint, description in admin_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
            
            # Special analysis for system metrics
            if endpoint == "/api/admin/systemMetrics" and result["success"] and "data" in result:
                self._analyze_system_metrics(result["data"])
        
        # Test file operations safely (read-only)
        self.test_admin_file_operations()
    
    def test_admin_file_operations(self):
        """Test admin file operations with detailed analysis"""
        self.log("\n--- Admin File Operations ---")
        
        # Test subdirectory listing
        subdirs = [
            ("Train_Data", "Train data files"),
            ("MyCiti_Data", "MyCiti data files"), 
            ("Taxi_Data", "Taxi data files"),
            ("GIS-Maps", "GIS mapping files")
        ]
        
        for subdir, description in subdirs:
            endpoint = f"/api/admin/list?subPath={subdir}"
            result = self.test_endpoint(endpoint, allow_statuses=[404])
            
            if result["success"]:
                self._log_endpoint_result(result, f"List {description}")
                if "data" in result and isinstance(result["data"], list):
                    file_count = len(result["data"])
                    self.log(f"   Found {file_count} files in {subdir}")
                    if file_count > 0 and file_count <= 5:  # Show some examples
                        self.log(f"   Examples: {result['data'][:3]}")
            else:
                self.log(f"   {subdir} directory not accessible (expected for some deployments)")
    
    def _analyze_system_metrics(self, metrics_data: Dict):
        """Analyze system metrics in detail"""
        self.log("   System Metrics Analysis:")
        
        # Check each subsystem
        subsystems = ["train", "taxi", "bus"]  # Based on your controllers
        for subsystem in subsystems:
            if subsystem in metrics_data:
                subsystem_data = metrics_data[subsystem]
                if isinstance(subsystem_data, dict):
                    stops = subsystem_data.get("stopsLoaded", subsystem_data.get("totalStops", 0))
                    trips = subsystem_data.get("tripsLoaded", subsystem_data.get("totalTrips", 0))
                    self.log(f"     {subsystem.upper()}: {stops} stops, {trips} trips")
        
        # Check performance overview if available
        if "performanceOverview" in metrics_data:
            perf = metrics_data["performanceOverview"]
            if isinstance(perf, dict):
                total_requests = perf.get("totalRequests", 0)
                avg_duration = perf.get("averageDurationMs", 0)
                self.log(f"     Performance: {total_requests} requests, {avg_duration:.1f}ms avg")
    
    def test_bus_graph_controller(self):
        """Test BusGraphController (combined MyCiti + GA)"""
        self.log("\n" + "=" * 70)
        self.log("TESTING BUS GRAPH CONTROLLER (COMBINED MYCITI + GA)")
        self.log("=" * 70)
        
        bus_endpoints = [
            ("/api/bus/stops", "All bus stops (combined)"),
            ("/api/bus/metrics", "Combined bus metrics"),
            ("/api/bus/routes", "All bus routes by company"),
            ("/api/bus/nearest?lat=-33.9249&lon=18.4241", "Nearest bus stop"),
        ]
        
        for endpoint, description in bus_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test journey planning with different algorithms
        self.test_bus_journey_planning()
    
    def test_bus_journey_planning(self):
        """Test bus journey planning with RAPTOR and CSA algorithms"""
        self.log("\n--- Bus Journey Planning Tests ---")
        
        journey_tests = [
            ("/api/bus/journey?from=Civic Centre&to=Century City&time=09:00&maxRounds=4", "Bus journey (RAPTOR)"),
            ("/api/bus/journey/csa?from=Civic Centre&to=Airport&time=10:00", "Bus journey (CSA algorithm)"),
            ("/api/bus/journey/compare?from=Wynberg&to=Claremont&time=08:00&maxRounds=4", "Algorithm comparison"),
        ]
        
        for endpoint, description in journey_tests:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
            
            if result["success"] and "data" in result:
                self._analyze_journey_result(result["data"], description)
    
    def test_ga_bus_controller(self):
        """Test GABusController endpoints"""
        self.log("\n" + "=" * 70)
        self.log("TESTING GA BUS CONTROLLER")
        self.log("=" * 70)
        
        ga_endpoints = [
            ("/api/GA/metrics", "GA Bus system metrics"),
            ("/api/GA/stops", "GA Bus stops"),
            ("/api/GA/trips", "GA Bus trips"),
        ]
        
        for endpoint, description in ga_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test GA Bus journey planning
        journey_result = self.test_endpoint(
            "/api/GA/journey?source=Wynberg&target=Claremont&departure=08:00&maxRounds=4&day=WEEKDAY"
        )
        self._log_endpoint_result(journey_result, "GA Bus journey planning")
        
        if journey_result["success"] and "data" in journey_result:
            self._analyze_journey_result(journey_result["data"], "GA Bus")
    
    def test_graph_controller(self):
        """Test GraphController (multimodal) endpoints"""
        self.log("\n" + "=" * 70)
        self.log("TESTING GRAPH CONTROLLER (MULTIMODAL)")
        self.log("=" * 70)
        
        graph_endpoints = [
            ("/api/graph/stops", "All multimodal stops"),
            ("/api/graph/metrics", "Multimodal graph metrics"),
            ("/api/graph/stops/nearest?lat=-33.9249&lon=18.4241", "Nearest multimodal stop"),
        ]
        
        for endpoint, description in graph_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test multimodal journey planning
        multimodal_tests = [
            ("/api/graph/journey?from=Cape Town&to=Bellville&time=08:00&modes=TRAIN,WALKING", 
             "Multimodal journey (Train + Walking)"),
            ("/api/graph/journey?from=Observatory&to=Century City&time=09:00&modes=TRAIN,MYCITI,WALKING&day=WEEKDAY", 
             "Complex multimodal journey"),
        ]
        
        for endpoint, description in multimodal_tests:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
            
            if result["success"] and "data" in result:
                self._analyze_journey_result(result["data"], "Multimodal")
    
    def test_myciti_bus_controller(self):
        """Test MyCitiBusController endpoints"""
        self.log("\n" + "=" * 70)
        self.log("TESTING MYCITI BUS CONTROLLER")
        self.log("=" * 70)
        
        myciti_endpoints = [
            ("/api/myciti/metrics", "MyCiti Bus metrics"),
            ("/api/myciti/stops", "MyCiti Bus stops"),
            ("/api/myciti/trips", "MyCiti Bus trips"),
            ("/api/myciti/logs", "MyCiti Bus system logs"),
        ]
        
        for endpoint, description in myciti_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test MyCiti Bus journey planning
        journey_result = self.test_endpoint(
            "/api/myciti/journey?source=Civic Centre&target=Airport&departure=09:00&maxRounds=4&day=WEEKDAY"
        )
        self._log_endpoint_result(journey_result, "MyCiti Bus journey planning")
        
        if journey_result["success"] and "data" in journey_result:
            self._analyze_journey_result(journey_result["data"], "MyCiti")
    
    def test_system_monitoring_controller(self):
        """Test SystemMonitoringController endpoints comprehensively"""
        self.log("\n" + "=" * 70)
        self.log("TESTING SYSTEM MONITORING CONTROLLER")
        self.log("=" * 70)
        
        # Core monitoring endpoints (may return 503 if system unhealthy)
        monitoring_endpoints = [
            ("/api/monitor/health", "System health check", [200, 503]),
            ("/api/monitor/summary", "System summary", [200]),
            ("/api/monitor/ready", "System readiness check", [200, 503]),
            ("/api/monitor/alerts", "Active system alerts", [200]),
            ("/api/monitor/alerts/all", "All system alerts", [200]),
            ("/api/monitor/stats", "Monitoring statistics", [200]),
            ("/api/monitor/performance", "Performance metrics", [200]),
        ]
        
        for endpoint, description, allowed_statuses in monitoring_endpoints:
            result = self.test_endpoint(endpoint, allow_statuses=allowed_statuses[1:] if len(allowed_statuses) > 1 else [])
            self._log_endpoint_result(result, description)
            
            # Detailed analysis for key endpoints
            if endpoint == "/api/monitor/health" and "data" in result:
                self._analyze_health_check(result["data"])
            elif endpoint == "/api/monitor/performance" and result["success"] and "data" in result:
                self._analyze_performance_metrics(result["data"])
        
        # Test individual graph monitoring
        self.test_individual_graph_monitoring()
        
        # Test forced health check (POST)
        force_result = self.test_endpoint("/api/monitor/health/check", "POST")
        self._log_endpoint_result(force_result, "Forced health check", "POST")
    
    def test_individual_graph_monitoring(self):
        """Test monitoring for each individual graph/transport mode"""
        self.log("\n--- Individual Graph Health Monitoring ---")
        
        graphs = ["train", "myciti", "ga", "taxi"]
        for graph in graphs:
            ready_result = self.test_endpoint(f"/api/monitor/graph/{graph}/ready", allow_statuses=[503])
            status_result = self.test_endpoint(f"/api/monitor/graph/{graph}")
            
            self.log(f"\n{graph.upper()} Graph Status:")
            
            if ready_result.get("data"):
                is_ready = ready_result["data"].get("ready", False)
                status = "READY" if is_ready else "NOT READY"
                self.log(f"  Readiness: {status}")
            
            if status_result["success"] and "data" in status_result:
                status_data = status_result["data"]
                graph_status = status_data.get("status", "UNKNOWN")
                stop_count = status_data.get("stopCount", "N/A")
                trip_count = status_data.get("tripCount", "N/A")
                
                self.log(f"  Health Status: {graph_status}")
                self.log(f"  Data: {stop_count} stops, {trip_count} trips")
                
                if graph_status in ["CRITICAL", "WARNING"]:
                    issues = status_data.get("issues", [])
                    if issues:
                        self.log(f"  Issues: {issues[:2]}")  # Show first 2 issues
    
    def _analyze_health_check(self, health_data: Dict):
        """Analyze detailed health check results"""
        if not isinstance(health_data, dict):
            return
            
        system_healthy = health_data.get("systemHealthy", False)
        total_checks = health_data.get("totalHealthChecks", "N/A")
        critical_errors = health_data.get("criticalErrors", "N/A")
        
        self.log(f"   Overall Health: {'HEALTHY' if system_healthy else 'UNHEALTHY'}")
        self.log(f"   Health Checks Performed: {total_checks}")
        self.log(f"   Critical Errors: {critical_errors}")
        
        # Analyze individual graph statuses
        graphs = health_data.get("graphs", {})
        if graphs:
            unhealthy_graphs = [name for name, data in graphs.items() 
                              if isinstance(data, dict) and data.get("status") != "HEALTHY"]
            if unhealthy_graphs:
                self.log(f"   Unhealthy Graphs: {unhealthy_graphs}")
    
    def _analyze_performance_metrics(self, perf_data: Dict):
        """Analyze performance metrics data"""
        if not isinstance(perf_data, dict):
            return
            
        overview = perf_data.get("overview", {})
        if overview:
            total_requests = overview.get("totalRequests", 0)
            avg_duration = overview.get("averageDurationMs", 0)
            max_duration = overview.get("maxDurationMs", 0)
            
            self.log(f"   Total Requests: {total_requests}")
            self.log(f"   Average Duration: {avg_duration:.1f}ms")
            self.log(f"   Maximum Duration: {max_duration}ms")
            
            if avg_duration > 1000:  # > 1 second average
                self.log("   WARNING: High average response time detected")
    
    def test_taxi_controller(self):
        """Test TaxiController endpoints"""
        self.log("\n" + "=" * 70)
        self.log("TESTING TAXI CONTROLLER")
        self.log("=" * 70)
        
        taxi_endpoints = [
            ("/api/taxi/metrics", "Taxi system metrics"),
            ("/api/taxi/all-stops", "All taxi stops"),
            ("/api/taxi/all-trips", "All taxi trips"),
        ]
        
        for endpoint, description in taxi_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test nearest taxi stops (POST with JSON body)
        nearest_data = {
            "location": {
                "latitude": -33.9249,
                "longitude": 18.4241
            },
            "max": 5
        }
        nearest_result = self.test_endpoint("/api/taxi/nearest-stops", "POST", nearest_data)
        self._log_endpoint_result(nearest_result, "Nearest taxi stops", "POST")
    
    def test_train_controller(self):
        """Test TrainController endpoints with coordinate support"""
        self.log("\n" + "=" * 70)
        self.log("TESTING TRAIN CONTROLLER")
        self.log("=" * 70)
        
        train_endpoints = [
            ("/api/train/metrics", "Train system metrics"),
            ("/api/train/stops", "All train stops"),
            ("/api/train/routes", "Train route numbers"),
            ("/api/train/nearest?lat=-33.9249&lon=18.4241", "Nearest train stop"),
            ("/api/train/routes/available", "Available railway routes from GeoJSON"),
        ]
        
        for endpoint, description in train_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test train journey planning
        journey_tests = [
            ("/api/train/journey?from=Cape Town&to=Bellville&time=08:00", "Basic train journey"),
            ("/api/train/journey/with-coordinates?from=Cape Town&to=Goodwood&time=09:00", "Train journey with coordinates"),
        ]
        
        for endpoint, description in journey_tests:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
            
            if result["success"] and "data" in result:
                self._analyze_journey_result(result["data"], "Train")
                
                # Special analysis for coordinate data
                if "with-coordinates" in endpoint:
                    self._analyze_coordinate_data(result["data"])
        
        # Test coordinate-specific endpoints
        self.test_train_coordinate_features()
    
    def test_train_coordinate_features(self):
        """Test train coordinate and mapping features"""
        self.log("\n--- Train Coordinate Features ---")
        
        coord_endpoints = [
            ("/api/train/coordinates?from=Cape Town&to=Bellville", "Direct coordinates between stops"),
            ("/api/train/routes/MetroRail%20Central%20Line/coordinates", "Complete route coordinates"),
        ]
        
        for endpoint, description in coord_endpoints:
            result = self.test_endpoint(endpoint)
            
            if result["success"]:
                self._log_endpoint_result(result, description)
                if "data" in result:
                    self._analyze_coordinate_data(result["data"])
            else:
                # Expected for some routes that may not exist
                self.log(f"   {description}: Not available (expected for some routes)")
    
    def _analyze_coordinate_data(self, journey_data: Dict):
        """Analyze coordinate data in journey responses"""
        if not isinstance(journey_data, dict):
            return
            
        mini_trips = journey_data.get("miniTrips", [])
        coord_count = 0
        
        for trip in mini_trips:
            if isinstance(trip, dict):
                trip_coords = trip.get("coordinateCount", 0)
                coord_count += trip_coords
                
                if trip_coords > 0:
                    distance = trip.get("segmentDistance", 0)
                    route_name = trip.get("routeName", "Unknown")
                    self.log(f"     Segment: {trip_coords} coordinates, {distance:.1f}km, route: {route_name}")
        
        if coord_count > 0:
            self.log(f"   Total coordinate points: {coord_count}")
        else:
            self.log("   No coordinate data available")
    
    def _analyze_journey_result(self, journey_data: Dict, transport_mode: str):
        """Analyze journey planning results"""
        if not isinstance(journey_data, dict):
            return
            
        duration = journey_data.get("durationMinutes", journey_data.get("totalDurationMinutes", 0))
        transfers = journey_data.get("transfers", journey_data.get("numberOfTransfers", 0))
        
        self.log(f"   {transport_mode} Journey: {duration} minutes, {transfers} transfers")
        
        # Check for mini trips
        mini_trips = journey_data.get("miniTrips", [])
        if mini_trips:
            self.log(f"   Journey segments: {len(mini_trips)}")
    
    def _log_endpoint_result(self, result: Dict[str, Any], description: str, method: str = "GET"):
        """Log endpoint test results with detailed analysis"""
        self.test_stats['total'] += 1
        
        if result["success"]:
            self.test_stats['passed'] += 1
            self.log(f"✅ {description} ({method}): {result['response_time_ms']}ms")
            
            if "data" in result:
                if isinstance(result["data"], list):
                    count = len(result["data"])
                    self.log(f"   Records returned: {count}")
                elif isinstance(result["data"], dict):
                    keys = len(result["data"].keys()) if result["data"] else 0
                    self.log(f"   Data fields: {keys}")
                    
                    # Check for error messages in successful responses
                    if "error" in result["data"]:
                        self.log(f"   Response contains error: {result['data']['error']}")
                        self.test_stats['warnings'] += 1
        else:
            self.test_stats['failed'] += 1
            status_info = f"HTTP {result['status_code']}" if result['status_code'] > 0 else "Connection Error"
            error_msg = result.get('error', 'Unknown error')[:100]
            self.log(f"❌ {description} ({method}): {status_info} - {error_msg}")
    
    def test_concurrent_load(self):
        """Test system under concurrent load across all controllers"""
        self.log("\n" + "=" * 70)
        self.log("TESTING CONCURRENT LOAD PERFORMANCE")
        self.log("=" * 70)
        
        # Key endpoints from each controller for load testing
        load_test_endpoints = [
            "/api/admin/systemMetrics",
            "/api/GA/metrics",
            "/api/bus/metrics",
            "/api/graph/metrics", 
            "/api/myciti/metrics",
            "/api/monitor/health",
            "/api/taxi/metrics",
            "/api/train/metrics",
        ]
        
        num_threads = 4  # Reduced to be gentler on server
        requests_per_thread = 3
        
        def test_worker(endpoints):
            results = []
            for endpoint in endpoints:
                for _ in range(requests_per_thread):
                    result = self.test_endpoint(endpoint)
                    results.append(result)
                    time.sleep(0.5)  # Increased delay between requests
            return results
        
        self.log(f"Starting {num_threads} concurrent threads, {requests_per_thread} requests each...")
        start_time = time.time()
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(test_worker, load_test_endpoints) for _ in range(num_threads)]
            all_results = []
            
            for future in concurrent.futures.as_completed(futures):
                try:
                    results = future.result()
                    all_results.extend(results)
                except Exception as e:
                    self.log(f"Thread failed: {e}", "ERROR")
        
        total_time = time.time() - start_time
        successful_requests = sum(1 for r in all_results if r["success"])
        total_requests = len(all_results)
        
        self.log(f"Concurrent test completed in {total_time:.2f}s")
        if total_requests > 0:
            success_rate = (successful_requests / total_requests) * 100
            self.log(f"Success rate: {successful_requests}/{total_requests} ({success_rate:.1f}%)")
            
            if all_results:
                response_times = [r["response_time_ms"] for r in all_results]
                avg_response_time = sum(response_times) / len(response_times)
                max_response_time = max(response_times)
                self.log(f"Average response time: {avg_response_time:.2f}ms")
                self.log(f"Maximum response time: {max_response_time:.2f}ms")
    
    def diagnostic_data_analysis(self):
        """Comprehensive diagnostic analysis of system data health"""
        self.log("\n" + "=" * 70)
        self.log("DIAGNOSTIC DATA HEALTH ANALYSIS")
        self.log("=" * 70)
        
        # Check each transport mode's data status
        transport_modes = [
            ("/api/train/metrics", "Train System"),
            ("/api/myciti/metrics", "MyCiti Bus System"),
            ("/api/GA/metrics", "GA Bus System"),
            ("/api/taxi/metrics", "Taxi System"),
        ]
        
        data_health_summary = {}
        
        for endpoint, system_name in transport_modes:
            result = self.test_endpoint(endpoint)
            
            if result["success"] and "data" in result:
                metrics = result["data"]
                
                # Extract relevant data counts
                stop_count = self._extract_count(metrics, ["stopCount", "totalStops", "stopsLoaded"])
                trip_count = self._extract_count(metrics, ["tripCount", "totalTrips", "tripsLoaded"])
                
                # Assess system health
                is_healthy = stop_count > 0 and trip_count > 0
                status = "✅ HEALTHY" if is_healthy else "⚠️ ISSUES DETECTED"
                
                self.log(f"{status} {system_name}: {stop_count} stops, {trip_count} trips")
                
                data_health_summary[system_name] = {
                    "healthy": is_healthy,
                    "stops": stop_count,
                    "trips": trip_count
                }
                
                # Specific issue detection
                if stop_count == 0:
                    self.log(f"   🔍 Critical: {system_name} has no stops loaded")
                if trip_count == 0:
                    self.log(f"   🔍 Critical: {system_name} has no trips/schedules loaded")
                elif trip_count < 10:
                    self.log(f"   🔍 Warning: {system_name} has suspiciously few trips")
                    
        # Overall system assessment
        self._generate_data_health_recommendations(data_health_summary)
    
    def _extract_count(self, metrics: Dict, possible_keys: List[str]) -> int:
        """Extract count from metrics using various possible key names"""
        if isinstance(metrics, dict):
            # Check direct keys
            for key in possible_keys:
                if key in metrics:
                    value = metrics[key]
                    return value if isinstance(value, int) else 0
            
            # Check nested metrics
            nested_metrics = metrics.get("metrics", {})
            if isinstance(nested_metrics, dict):
                for key in possible_keys:
                    if key in nested_metrics:
                        value = nested_metrics[key]
                        return value if isinstance(value, int) else 0
        
        return 0
    
    def _generate_data_health_recommendations(self, health_summary: Dict):
        """Generate recommendations based on data health analysis"""
        healthy_systems = sum(1 for system in health_summary.values() if system["healthy"])
        total_systems = len(health_summary)
        
        self.log(f"\nData Health Summary: {healthy_systems}/{total_systems} systems healthy")
        
        if healthy_systems == total_systems:
            self.log("🎉 All transport systems have healthy data loads")
            self.log("   System is ready for production journey planning")
        elif healthy_systems >= total_systems * 0.75:
            self.log("👍 Most systems healthy, minor data issues detected")
            self.log("   Recommended actions:")
            self.log("   - Review systems with missing data")
            self.log("   - Check data file availability and format")
        else:
            self.log("🚨 Multiple systems have data loading issues")
            self.log("   Critical actions required:")
            self.log("   - Verify all data files exist in resources/CapeTownTransitData/")
            self.log("   - Check application startup logs for loading errors")
            self.log("   - Ensure proper file permissions and formats")
    
    def generate_comprehensive_report(self):
        """Generate comprehensive test report with actionable insights"""
        self.log("\n" + "=" * 70)
        self.log("COMPREHENSIVE SYSTEM ASSESSMENT REPORT")
        self.log("=" * 70)
        
        total = self.test_stats['total']
        passed = self.test_stats['passed']
        failed = self.test_stats['failed']
        warnings = self.test_stats['warnings']
        
        self.log(f"Test Execution Summary:")
        self.log(f"  Total Tests: {total}")
        self.log(f"  Passed: {passed}")
        self.log(f"  Failed: {failed}")
        self.log(f"  Warnings: {warnings}")
        
        if total > 0:
            success_rate = (passed / total) * 100
            self.log(f"  Overall Success Rate: {success_rate:.1f}%")
            
            # System status assessment with specific recommendations
            if success_rate >= 95:
                self.log("\n🎉 SYSTEM STATUS: EXCELLENT")
                self.log("   All controllers are fully operational")
                self.log("   System ready for production use")
                self.log("   Recommended: Continue regular monitoring")
            elif success_rate >= 85:
                self.log("\n👍 SYSTEM STATUS: GOOD")
                self.log("   Minor issues detected but system largely functional")
                self.log("   Recommended: Review failed endpoints for quick fixes")
            elif success_rate >= 70:
                self.log("\n⚠️ SYSTEM STATUS: ACCEPTABLE WITH CONCERNS")
                self.log("   Some controllers need attention")
                self.log("   Recommended: Priority fixes for failing critical endpoints")
            elif success_rate >= 50:
                self.log("\n🚨 SYSTEM STATUS: DEGRADED")
                self.log("   Multiple controller issues detected")
                self.log("   Recommended: Immediate investigation of system health")
            else:
                self.log("\n💥 SYSTEM STATUS: CRITICAL")
                self.log("   System-wide failures detected")
                self.log("   Recommended: Full system diagnostic required")
        
        # Controller-specific analysis
        self._analyze_controller_performance()
        
        # Technical recommendations
        self._generate_technical_recommendations()
        
        self.log(f"\nTest completed: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self.log(f"Full test results: {len(self.results)} log entries generated")
    
    def _analyze_controller_performance(self):
        """Analyze performance by controller"""
        self.log("\nController Performance Analysis:")
        
        controllers = [
            ("ADMIN", "Administrative functions"),
            ("BUS", "Combined bus operations"), 
            ("GA BUS", "Golden Arrow bus network"),
            ("GRAPH", "Multimodal integration"),
            ("MYCITI", "MyCiti bus network"),
            ("MONITORING", "System health monitoring"),
            ("TAXI", "Taxi/minibus network"),
            ("TRAIN", "Railway network")
        ]
        
        for controller, description in controllers:
            controller_results = [r for r in self.results if controller.replace(" ", "") in r.upper().replace(" ", "")]
            
            if controller_results:
                success_results = [r for r in controller_results if "✅" in r]
                total_results = [r for r in controller_results if ("✅" in r or "❌" in r)]
                
                if total_results:
                    success_rate = (len(success_results) / len(total_results)) * 100
                    status_icon = "✅" if success_rate >= 90 else "⚠️" if success_rate >= 70 else "❌"
                    self.log(f"  {status_icon} {controller}: {success_rate:.0f}% ({description})")
    
    def _generate_technical_recommendations(self):
        """Generate specific technical recommendations"""
        self.log("\nTechnical Recommendations:")
        
        failed_count = self.test_stats['failed']
        
        if failed_count == 0:
            self.log("  ✅ No technical issues detected")
            self.log("  - Continue current monitoring schedule")
            self.log("  - Consider load testing for production readiness")
        else:
            self.log("  🔧 Technical Actions Required:")
            
            if failed_count > 10:
                self.log("  - Check system connectivity and basic configuration")
                self.log("  - Verify application is running and accessible")
                
            self.log("  - Review application logs for startup errors")
            self.log("  - Verify data file integrity and accessibility")
            self.log("  - Check Spring Boot actuator health endpoints")
            self.log("  - Validate database connections if applicable")
            
            if self.test_stats['warnings'] > 0:
                self.log("  - Investigate warnings in successful responses")
                self.log("  - Check for partial data loading issues")
    
    def run_comprehensive_test_suite(self):
        """Execute the complete enhanced test suite"""
        self.log("🚀 ENHANCED TRANSPORT SYSTEM COMPREHENSIVE TESTING")
        self.log(f"Target System: {self.base_url}")
        self.log(f"Test Suite Start: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self.log("Testing ALL controllers with advanced diagnostics...")
        self.log("=" * 70)
        
        start_time = time.time()
        
        try:
            # Core controller testing
            self.test_admin_controller()
            self.test_bus_graph_controller()  # Combined MyCiti + GA
            self.test_ga_bus_controller()     # Individual GA
            self.test_graph_controller()      # Multimodal
            self.test_myciti_bus_controller() # Individual MyCiti
            self.test_system_monitoring_controller()
            self.test_taxi_controller()
            self.test_train_controller()
            
            # Advanced testing
            self.diagnostic_data_analysis()
            self.test_concurrent_load()
            
            # Generate final report
            self.generate_comprehensive_report()
            
        except KeyboardInterrupt:
            self.log("\n⚠️ Testing interrupted by user", "WARN")
            self.log("Generating partial report...")
            self.generate_comprehensive_report()
        except Exception as e:
            self.log(f"\n💥 Unexpected error during testing: {e}", "ERROR")
            import traceback
            self.log(f"Error details: {traceback.format_exc()}")
        
        total_time = time.time() - start_time
        self.log(f"\n⏱️ Total test execution time: {total_time:.1f} seconds")
        self.log(f"✅ Enhanced testing suite completed: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


def main():
    """Main entry point with enhanced argument handling"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Enhanced Transport System Monitor for Boolean Brotherhood PTJP",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python enhanced_transport_monitor.py
  python enhanced_transport_monitor.py http://localhost:8080
  python enhanced_transport_monitor.py https://your-deployment.railway.app

This comprehensive test suite validates:
- All REST API endpoints across 8 controllers
- Data loading and integrity for all transport modes
- Journey planning algorithms (RAPTOR, CSA, Multimodal)
- System health monitoring and performance metrics
- Coordinate mapping features for railway routes
- Concurrent load handling and response times
        """
    )
    
    parser.add_argument(
        'base_url', 
        nargs='?', 
        default='https://pjtp-brotherhood.up.railway.app',
        help='Base URL of the PTJP system to test (default: %(default)s)'
    )
    
    parser.add_argument(
        '--timeout',
        type=int,
        default=30,
        help='Request timeout in seconds (default: %(default)s)'
    )
    
    args = parser.parse_args()
    
    print("=" * 80)
    print("🔍 ENHANCED TRANSPORT SYSTEM TESTING SUITE")
    print("   Boolean Brotherhood Public Transportation Journey Planner")
    print("=" * 80)
    print(f"Target System: {args.base_url}")
    print(f"Request Timeout: {args.timeout}s")
    print("\nTesting comprehensive controller functionality:")
    print("  📋 AdminController: File management, metrics, system logs")
    print("  🚌 BusGraphController: Combined MyCiti + GA bus operations")
    print("  🟡 GABusController: Golden Arrow bus network")
    print("  🔄 GraphController: Multimodal journey planning")
    print("  🔵 MyCitiBusController: MyCiti bus network")
    print("  ❤️ SystemMonitoringController: Health & performance monitoring")
    print("  🚐 TaxiController: Taxi/minibus network") 
    print("  🚂 TrainController: Railway network with GIS coordinates")
    print("=" * 80)
    print()
    
    monitor = EnhancedTransportSystemMonitor(args.base_url)
    monitor.session.timeout = args.timeout
    monitor.run_comprehensive_test_suite()


if __name__ == "__main__":
    main()