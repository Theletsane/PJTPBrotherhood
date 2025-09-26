#!/usr/bin/env python3
"""
Enhanced Transport System Monitor Test Script

This script comprehensively tests all controllers and monitoring capabilities 
of the Public Transportation Journey Planner system.

Covers:
- AdminController
- GABusController  
- GraphController
- MyCitiBusController
- SystemMonitoringController
- TaxiController
- TrainController

Usage: python monitor_test.py [base_url]
"""

import requests
import json
import time
import sys
from datetime import datetime
from typing import Dict, Any, List
import concurrent.futures
import threading

class TransportSystemMonitor:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.session.timeout = 15
        self.results = []
        self.lock = threading.Lock()
        
    def log(self, message: str, level: str = "INFO"):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with self.lock:
            log_entry = f"[{timestamp}] {level}: {message}"
            print(log_entry)
            self.results.append(log_entry)
    
    def test_endpoint(self, endpoint: str, method: str = "GET", data: Dict = None, expected_status: int = 200) -> Dict[str, Any]:
        """Test a single endpoint and return results"""
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
            
            result = {
                "endpoint": endpoint,
                "method": method,
                "status_code": response.status_code,
                "response_time_ms": round(response_time, 2),
                "success": response.status_code == expected_status,
                "content_length": len(response.content),
                "timestamp": datetime.now().isoformat()
            }
            
            if response.status_code == expected_status:
                try:
                    data = response.json()
                    result["data"] = data
                    result["has_data"] = bool(data)
                except:
                    result["data"] = response.text[:500]  # First 500 chars
                    result["has_data"] = bool(response.text)
            else:
                result["error"] = response.text[:500]
            
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
        """Test AdminController endpoints"""
        self.log("=" * 60)
        self.log("TESTING ADMIN CONTROLLER")
        self.log("=" * 60)
        
        admin_endpoints = [
            ("/api/admin/list", "List data files"),
            ("/api/admin/systemMetrics", "System metrics"),
            ("/api/admin/GetFileInUse", "Files in use"),
            ("/api/admin/MostRecentCall", "Recent API calls"),
            ("/api/admin/systemLogs?limit=10", "System logs (limited)"),
        ]
        
        for endpoint, description in admin_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
    
    def test_ga_bus_controller(self):
        """Test GABusController endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING GA BUS CONTROLLER")
        self.log("=" * 60)
        
        ga_endpoints = [
            ("/api/GA/metrics", "GA Bus metrics"),
            ("/api/GA/stops", "GA Bus stops"),
            ("/api/GA/trips", "GA Bus trips"),
        ]
        
        for endpoint, description in ga_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test GA Bus journey planning
        journey_result = self.test_endpoint(
            "/api/GA/journey?source=Wynberg&target=Claremont&departure=08:00&maxRounds=4",
            "GET"
        )
        self._log_endpoint_result(journey_result, "GA Bus journey planning")
    
    def test_graph_controller(self):
        """Test GraphController endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING GRAPH CONTROLLER (MULTIMODAL)")
        self.log("=" * 60)
        
        graph_endpoints = [
            ("/api/graph/stops", "All multimodal stops"),
            ("/api/graph/metrics", "Graph metrics"),
            ("/api/graph/stops/nearest?lat=-33.9249&lon=18.4241", "Nearest stop lookup"),
        ]
        
        for endpoint, description in graph_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test multimodal journey planning
        journey_result = self.test_endpoint(
            "/api/graph/journey?from=Cape Town&to=Bellville&time=08:00&modes=TRAIN,WALKING",
            "GET"
        )
        self._log_endpoint_result(journey_result, "Multimodal journey planning")
    
    def test_myciti_bus_controller(self):
        """Test MyCitiBusController endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING MYCITI BUS CONTROLLER")
        self.log("=" * 60)
        
        myciti_endpoints = [
            ("/api/myciti/metrics", "MyCiti Bus metrics"),
            ("/api/myciti/stops", "MyCiti Bus stops"),
            ("/api/myciti/trips", "MyCiti Bus trips"),
            ("/api/myciti/logs", "MyCiti Bus logs"),
        ]
        
        for endpoint, description in myciti_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test MyCiti Bus journey planning
        journey_result = self.test_endpoint(
            "/api/myciti/journey?source=Civic Centre&target=Airport&departure=09:00&maxRounds=4",
            "GET"
        )
        self._log_endpoint_result(journey_result, "MyCiti Bus journey planning")
    
    def test_system_monitoring_controller(self):
        """Test SystemMonitoringController endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING SYSTEM MONITORING CONTROLLER")
        self.log("=" * 60)
        
        monitoring_endpoints = [
            ("/api/monitor/health", "System health check"),
            ("/api/monitor/summary", "System summary"),
            ("/api/monitor/ready", "System readiness"),
            ("/api/monitor/alerts", "Active alerts"),
            ("/api/monitor/alerts/all", "All alerts"),
            ("/api/monitor/stats", "Monitoring statistics"),
            ("/api/monitor/performance", "Performance metrics"),
        ]
        
        for endpoint, description in monitoring_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
            
            # Special handling for health check
            if endpoint == "/api/monitor/health" and result["success"] and "data" in result:
                health_data = result["data"]
                system_healthy = health_data.get("systemHealthy", False)
                self.log(f"   System Status: {'HEALTHY' if system_healthy else 'UNHEALTHY'}")
                self.log(f"   Health Checks: {health_data.get('totalHealthChecks', 'N/A')}")
                self.log(f"   Critical Errors: {health_data.get('criticalErrors', 'N/A')}")
        
        # Test graph-specific monitoring
        graphs = ["train", "myciti", "ga", "taxi"]
        for graph in graphs:
            ready_result = self.test_endpoint(f"/api/monitor/graph/{graph}/ready")
            status_result = self.test_endpoint(f"/api/monitor/graph/{graph}")
            
            self.log(f"\n{graph.upper()} Graph Status:")
            if ready_result["success"] and "data" in ready_result:
                is_ready = ready_result["data"].get("ready", False)
                self.log(f"  Ready: {'YES' if is_ready else 'NO'}")
            
            if status_result["success"] and "data" in status_result:
                status = status_result["data"]
                self.log(f"  Status: {status.get('status', 'UNKNOWN')}")
                self.log(f"  Stops: {status.get('stopCount', 'N/A')}")
                self.log(f"  Trips: {status.get('tripCount', 'N/A')}")
        
        # Test forced health check (POST)
        force_result = self.test_endpoint("/api/monitor/health/check", "POST")
        self._log_endpoint_result(force_result, "Forced health check", "POST")
    
    def test_taxi_controller(self):
        """Test TaxiController endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING TAXI CONTROLLER")
        self.log("=" * 60)
        
        taxi_endpoints = [
            ("/api/taxi/metrics", "Taxi metrics"),
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
        """Test TrainController endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING TRAIN CONTROLLER")
        self.log("=" * 60)
        
        train_endpoints = [
            ("/api/train/metrics", "Train metrics"),
            ("/api/train/stops", "All train stops"),
            ("/api/train/routes", "Train routes"),
            ("/api/train/nearest?lat=-33.9249&lon=18.4241", "Nearest train stop"),
            ("/api/train/routes/available", "Available railway routes"),
        ]
        
        for endpoint, description in train_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
        
        # Test train journey planning
        journey_result = self.test_endpoint(
            "/api/train/journey?from=Cape Town&to=Bellville&time=08:00",
            "GET"
        )
        self._log_endpoint_result(journey_result, "Train journey planning")
        
        # Test journey with coordinates
        coord_result = self.test_endpoint(
            "/api/train/journey/with-coordinates?from=Cape Town&to=Bellville&time=08:00",
            "GET"
        )
        self._log_endpoint_result(coord_result, "Train journey with coordinates")
    
    def _log_endpoint_result(self, result: Dict[str, Any], description: str, method: str = "GET"):
        """Helper method to log endpoint test results consistently"""
        if result["success"]:
            self.log(f"✅ {description} ({method}): {result['response_time_ms']}ms")
            if "data" in result:
                if isinstance(result["data"], list):
                    self.log(f"   Records: {len(result['data'])}")
                elif isinstance(result["data"], dict):
                    data_keys = len(result["data"].keys()) if result["data"] else 0
                    self.log(f"   Data keys: {data_keys}")
        else:
            status_info = f"HTTP {result['status_code']}" if result['status_code'] > 0 else "Connection Error"
            error_msg = result.get('error', 'Unknown error')[:100]
            self.log(f"❌ {description} ({method}): {status_info} - {error_msg}", "ERROR")
    
    def test_concurrent_load(self):
        """Test system under concurrent load across all controllers"""
        self.log("\n" + "=" * 60)
        self.log("TESTING CONCURRENT LOAD ACROSS ALL CONTROLLERS")
        self.log("=" * 60)
        
        # Key endpoints from each controller for load testing
        load_test_endpoints = [
            "/api/admin/systemMetrics",
            "/api/GA/metrics",
            "/api/graph/metrics", 
            "/api/myciti/metrics",
            "/api/monitor/health",
            "/api/taxi/metrics",
            "/api/train/metrics",
        ]
        
        num_threads = 6
        requests_per_thread = 4
        
        def test_worker(endpoints):
            results = []
            for endpoint in endpoints:
                for _ in range(requests_per_thread):
                    result = self.test_endpoint(endpoint)
                    results.append(result)
                    time.sleep(0.2)  # Small delay between requests
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
        self.log(f"Success rate: {successful_requests}/{total_requests} ({(successful_requests/total_requests*100):.1f}%)")
        
        if all_results:
            avg_response_time = sum(r["response_time_ms"] for r in all_results) / len(all_results)
            max_response_time = max(r["response_time_ms"] for r in all_results)
            self.log(f"Average response time: {avg_response_time:.2f}ms")
            self.log(f"Maximum response time: {max_response_time:.2f}ms")
    
    def test_journey_planning_integration(self):
        """Test journey planning across different transport modes"""
        self.log("\n" + "=" * 60)
        self.log("TESTING JOURNEY PLANNING INTEGRATION")
        self.log("=" * 60)
        
        # Test journey planning for each transport mode
        journey_tests = [
            ("/api/train/journey?from=Cape Town&to=Goodwood&time=08:00", "Train journey"),
            ("/api/myciti/journey?source=Civic Centre&target=Century City&departure=09:00", "MyCiti journey"),
            ("/api/GA/journey?source=Wynberg&target=Observatory&departure=10:00", "GA Bus journey"),
            ("/api/graph/journey?from=Cape Town&to=Bellville&time=08:00&modes=TRAIN,MYCITI,WALKING", "Multimodal journey"),
        ]
        
        for endpoint, description in journey_tests:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
            
            # Log journey details if successful
            if result["success"] and "data" in result and isinstance(result["data"], dict):
                data = result["data"]
                if "durationMinutes" in data:
                    self.log(f"   Duration: {data['durationMinutes']} minutes")
                if "transfers" in data:
                    self.log(f"   Transfers: {data.get('transfers', 0)}")
                elif "numberOfTransfers" in data:
                    self.log(f"   Transfers: {data.get('numberOfTransfers', 0)}")
    
    def test_file_operations(self):
        """Test admin file operations (read-only tests)"""
        self.log("\n" + "=" * 60)
        self.log("TESTING ADMIN FILE OPERATIONS")
        self.log("=" * 60)
        
        # Test file listing and reading (safe operations)
        file_endpoints = [
            ("/api/admin/list", "List root data files"),
            ("/api/admin/list?subPath=Train", "List train data files"),
            ("/api/admin/list?subPath=MyCitiBus", "List MyCiti data files"),
        ]
        
        for endpoint, description in file_endpoints:
            result = self.test_endpoint(endpoint)
            self._log_endpoint_result(result, description)
    
    def generate_comprehensive_report(self):
        """Generate a comprehensive test report"""
        self.log("\n" + "=" * 60)
        self.log("COMPREHENSIVE SYSTEM TEST REPORT")
        self.log("=" * 60)
        
        # Count results by type
        success_count = sum(1 for r in self.results if "✅" in r)
        error_count = sum(1 for r in self.results if "❌" in r)
        total_tests = success_count + error_count
        
        self.log(f"Total Tests Executed: {total_tests}")
        self.log(f"Successful Tests: {success_count}")
        self.log(f"Failed Tests: {error_count}")
        
        if total_tests > 0:
            success_rate = (success_count / total_tests) * 100
            self.log(f"Overall Success Rate: {success_rate:.1f}%")
            
            # System status assessment
            if success_rate >= 95:
                self.log("🎉 SYSTEM STATUS: EXCELLENT - All controllers operational")
            elif success_rate >= 85:
                self.log("👍 SYSTEM STATUS: GOOD - Minor issues detected")
            elif success_rate >= 70:
                self.log("⚠️  SYSTEM STATUS: ACCEPTABLE - Some controllers need attention")
            elif success_rate >= 50:
                self.log("🚨 SYSTEM STATUS: DEGRADED - Multiple controller issues")
            else:
                self.log("💥 SYSTEM STATUS: CRITICAL - System-wide failures detected")
        
        # Controller-specific analysis
        controllers = ["ADMIN", "GA BUS", "GRAPH", "MYCITI", "MONITORING", "TAXI", "TRAIN"]
        for controller in controllers:
            controller_results = [r for r in self.results if controller in r.upper()]
            if controller_results:
                controller_success = sum(1 for r in controller_results if "✅" in r)
                controller_total = sum(1 for r in controller_results if ("✅" in r or "❌" in r))
                if controller_total > 0:
                    controller_rate = (controller_success / controller_total) * 100
                    status = "✅" if controller_rate >= 90 else "⚠️" if controller_rate >= 70 else "❌"
                    self.log(f"{status} {controller} Controller: {controller_rate:.0f}% success rate")
        
        self.log("\nRecommendations:")
        if error_count == 0:
            self.log("- All controllers are fully operational")
            self.log("- System is ready for production traffic")
            self.log("- Continue regular monitoring")
        elif success_rate >= 85:
            self.log("- System is mostly healthy with minor issues")
            self.log("- Review failed endpoints for quick fixes")
            self.log("- Monitor failing services closely")
        else:
            self.log("- Critical issues detected across multiple controllers")
            self.log("- Immediate attention required for failing services")
            self.log("- Check service connectivity and configuration")
            self.log("- Verify data file availability and permissions")
            
        self.log(f"\nTest completed at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    def run_full_test_suite(self):
        """Execute the complete test suite for all controllers"""
        self.log("🚀 STARTING COMPREHENSIVE CONTROLLER TESTING")
        self.log(f"Target System: {self.base_url}")
        self.log(f"Test Start Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self.log("Testing all controllers: Admin, GA Bus, Graph, MyCiti, Monitoring, Taxi, Train")
        
        try:
            # Test all controllers systematically
            self.test_admin_controller()
            self.test_ga_bus_controller()
            self.test_graph_controller()
            self.test_myciti_bus_controller()
            self.test_system_monitoring_controller()
            self.test_taxi_controller()
            self.test_train_controller()
            
            # Integration and load testing
            self.test_journey_planning_integration()
            self.test_file_operations()
            self.test_concurrent_load()
            
            # Generate comprehensive report
            self.generate_comprehensive_report()
            
        except KeyboardInterrupt:
            self.log("\n⚠️ Testing interrupted by user", "WARN")
        except Exception as e:
            self.log(f"\n💥 Unexpected error during testing: {e}", "ERROR")
        
        self.log(f"\n✅ Testing suite completed at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


def main():
    """Main entry point"""
    base_url = sys.argv[1] if len(sys.argv) > 1 else "https://pjtp-brotherhood.up.railway.app"
    
    print("=" * 80)
    print("🔍 ENHANCED TRANSPORT SYSTEM TESTING SUITE")
    print("=" * 80)
    print(f"Testing system at: {base_url}")
    print("This will comprehensively test ALL controllers and their endpoints:")
    print("- AdminController: File management and system metrics")
    print("- GABusController: Golden Arrow bus network")  
    print("- GraphController: Multimodal journey planning")
    print("- MyCitiBusController: MyCiti bus network")
    print("- SystemMonitoringController: Health and performance monitoring")
    print("- TaxiController: Taxi/minibus network")
    print("- TrainController: Railway network")
    print("=" * 80)
    
    monitor = TransportSystemMonitor(base_url)
    monitor.run_full_test_suite()


if __name__ == "__main__":
    main()