#!/usr/bin/env python3

"""
Transport System Monitor Test Script

This script comprehensively tests the monitoring capabilities of the 
Public Transportation Journey Planner system.

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
        self.session.timeout = 10
        self.results = []
        self.lock = threading.Lock()
        
    def log(self, message: str, level: str = "INFO"):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with self.lock:
            log_entry = f"[{timestamp}] {level}: {message}"
            print(log_entry)
            self.results.append(log_entry)
    
    def test_endpoint(self, endpoint: str, expected_status: int = 200) -> Dict[str, Any]:
        """Test a single endpoint and return results"""
        url = f"{self.base_url}{endpoint}"
        start_time = time.time()
        
        try:
            response = self.session.get(url)
            response_time = (time.time() - start_time) * 1000  # ms
            
            result = {
                "endpoint": endpoint,
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
                "status_code": 0,
                "response_time_ms": (time.time() - start_time) * 1000,
                "success": False,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }
    
    def test_health_monitoring(self):
        """Test all health monitoring endpoints"""
        self.log("=" * 60)
        self.log("TESTING HEALTH MONITORING ENDPOINTS")
        self.log("=" * 60)
        
        health_endpoints = [
            "/api/monitor/health",
            "/api/monitor/summary", 
            "/api/monitor/ready",
            "/api/monitor/alerts",
            "/api/monitor/alerts/all",
            "/api/monitor/stats"
        ]
        
        for endpoint in health_endpoints:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"✅ {endpoint}: {result['response_time_ms']}ms")
                if endpoint == "/api/monitor/health" and "data" in result:
                    health_data = result["data"]
                    system_healthy = health_data.get("systemHealthy", False)
                    self.log(f"   System Status: {'HEALTHY' if system_healthy else 'UNHEALTHY'}")
                    self.log(f"   Health Checks: {health_data.get('totalHealthChecks', 'N/A')}")
                    self.log(f"   Critical Errors: {health_data.get('criticalErrors', 'N/A')}")
            else:
                self.log(f"❌ {endpoint}: {result.get('error', 'Failed')}", "ERROR")
    
    def test_graph_monitoring(self):
        """Test graph-specific monitoring"""
        self.log("\n" + "=" * 60)
        self.log("TESTING GRAPH-SPECIFIC MONITORING")
        self.log("=" * 60)
        
        graphs = ["train", "bus", "taxi"]
        
        for graph in graphs:
            self.log(f"\nTesting {graph.upper()} Graph:")
            
            # Test graph readiness
            ready_result = self.test_endpoint(f"/api/monitor/graph/{graph}/ready")
            if ready_result["success"] and "data" in ready_result:
                is_ready = ready_result["data"].get("ready", False)
                self.log(f"  Ready Status: {'✅ READY' if is_ready else '❌ NOT READY'}")
            
            # Test graph status
            status_result = self.test_endpoint(f"/api/monitor/graph/{graph}")
            if status_result["success"] and "data" in status_result:
                status = status_result["data"]
                self.log(f"  Status: {status.get('status', 'UNKNOWN')}")
                self.log(f"  Stops: {status.get('stopCount', 'N/A')}")
                self.log(f"  Trips: {status.get('tripCount', 'N/A')}")
                self.log(f"  Load Time: {status.get('loadTimeMs', 'N/A')}ms")
    
    def test_admin_monitoring(self):
        """Test admin monitoring endpoints"""
        self.log("\n" + "=" * 60)
        self.log("TESTING ADMIN MONITORING ENDPOINTS")  
        self.log("=" * 60)
        
        admin_endpoints = [
            "/api/admin/systemMetrics",
            "/api/admin/GetFileInUse",
            "/api/admin/MostRecentCall"
        ]
        
        for endpoint in admin_endpoints:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"✅ {endpoint}: {result['response_time_ms']}ms")
                if "data" in result and isinstance(result["data"], (dict, list)):
                    data_size = len(result["data"]) if isinstance(result["data"], list) else len(result["data"].keys())
                    self.log(f"   Data elements: {data_size}")
            else:
                self.log(f"❌ {endpoint}: {result.get('error', 'Failed')}", "ERROR")
    
    def test_service_endpoints(self):
        """Test that main service endpoints are responding"""
        self.log("\n" + "=" * 60)
        self.log("TESTING MAIN SERVICE ENDPOINTS")
        self.log("=" * 60)
        
        service_endpoints = [
            ("/api/train/stops", "Train stops"),
            ("/api/train/metrics", "Train metrics"),
            ("/api/myciti/stops", "MyCiti stops"), 
            ("/api/myciti/metrics", "MyCiti metrics"),
            ("/api/taxi/all-stops", "Taxi stops"),
            ("/api/taxi/metrics", "Taxi metrics")
        ]
        
        for endpoint, description in service_endpoints:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"✅ {description}: {result['response_time_ms']}ms")
                if "data" in result and isinstance(result["data"], list):
                    self.log(f"   Records: {len(result['data'])}")
            else:
                self.log(f"❌ {description}: {result.get('error', 'Failed')}", "ERROR")
    
    def test_concurrent_monitoring(self):
        """Test monitoring under concurrent load"""
        self.log("\n" + "=" * 60)
        self.log("TESTING CONCURRENT MONITORING LOAD")
        self.log("=" * 60)
        
        endpoints_to_test = [
            "/api/monitor/health",
            "/api/monitor/ready", 
            "/api/admin/systemMetrics",
            "/api/train/metrics",
            "/api/myciti/metrics",
            "/api/taxi/metrics"
        ]
        
        num_threads = 5
        requests_per_thread = 3
        
        def test_worker(endpoints):
            results = []
            for endpoint in endpoints:
                for _ in range(requests_per_thread):
                    result = self.test_endpoint(endpoint)
                    results.append(result)
                    time.sleep(0.1)  # Small delay
            return results
        
        self.log(f"Starting {num_threads} concurrent threads, {requests_per_thread} requests each...")
        start_time = time.time()
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(test_worker, endpoints_to_test) for _ in range(num_threads)]
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
            self.log(f"Average response time: {avg_response_time:.2f}ms")
    
    def test_forced_health_check(self):
        """Test forcing a health check"""
        self.log("\n" + "=" * 60)
        self.log("TESTING FORCED HEALTH CHECK")
        self.log("=" * 60)
        
        # POST request for forced health check
        url = f"{self.base_url}/api/monitor/health/check"
        start_time = time.time()
        
        try:
            response = self.session.post(url)
            response_time = (time.time() - start_time) * 1000
            
            if response.status_code == 200:
                self.log(f"✅ Forced health check: {response_time:.2f}ms")
                data = response.json()
                self.log(f"   System Status: {data.get('systemStatus', 'Unknown')}")
                self.log(f"   Active Alerts: {len(data.get('activeAlerts', []))}")
            else:
                self.log(f"❌ Forced health check failed: HTTP {response.status_code}", "ERROR")
        except Exception as e:
            self.log(f"❌ Forced health check error: {e}", "ERROR")
    
    def test_journey_endpoints(self):
        """Test journey planning endpoints to ensure they're monitored"""
        self.log("\n" + "=" * 60)
        self.log("TESTING JOURNEY PLANNING (MONITORING IMPACT)")
        self.log("=" * 60)
        
        # Test some journey endpoints to trigger endpoint logging
        journey_tests = [
            ("/api/train/journey?from=Cape Town&to=Bellville&time=08:00", "Train journey"),
            ("/api/myciti/journey?source=Civic Centre&target=Airport&departure=09:00", "MyCiti journey"), 
            ("/api/taxi/all-stops", "Taxi stops lookup")
        ]
        
        for endpoint, description in journey_tests:
            result = self.test_endpoint(endpoint)
            if result["success"]:
                self.log(f"✅ {description}: {result['response_time_ms']}ms")
            else:
                self.log(f"❌ {description}: {result.get('error', 'Failed')}", "WARN")
        
        # Check if these calls were logged
        time.sleep(1)  # Give time for logging
        log_result = self.test_endpoint("/api/admin/MostRecentCall")
        if log_result["success"] and "data" in log_result:
            recent_calls = log_result["data"]
            self.log(f"Recent endpoint calls logged: {len(recent_calls)}")
    
    def generate_report(self):
        """Generate a summary report"""
        self.log("\n" + "=" * 60)
        self.log("MONITORING TEST SUMMARY REPORT")
        self.log("=" * 60)
        
        # Count results
        success_count = sum(1 for r in self.results if "✅" in r)
        error_count = sum(1 for r in self.results if "❌" in r)
        total_tests = success_count + error_count
        
        self.log(f"Total Tests: {total_tests}")
        self.log(f"Successful: {success_count}")
        self.log(f"Failed: {error_count}")
        
        if total_tests > 0:
            success_rate = (success_count / total_tests) * 100
            self.log(f"Success Rate: {success_rate:.1f}%")
            
            if success_rate >= 90:
                self.log("🎉 MONITORING SYSTEM STATUS: EXCELLENT")
            elif success_rate >= 75:
                self.log("👍 MONITORING SYSTEM STATUS: GOOD") 
            elif success_rate >= 50:
                self.log("⚠️  MONITORING SYSTEM STATUS: NEEDS ATTENTION")
            else:
                self.log("🚨 MONITORING SYSTEM STATUS: CRITICAL ISSUES")
        
        self.log("\nRecommendations:")
        if error_count == 0:
            self.log("- Monitoring system is fully operational")
            self.log("- Continue regular monitoring checks")
        else:
            self.log("- Review failed endpoints and fix connectivity issues")
            self.log("- Check service availability and configuration")
            self.log("- Verify all transportation graphs are properly loaded")
    
    def run_full_test_suite(self):
        """Run the complete monitoring test suite"""
        self.log("🚀 STARTING COMPREHENSIVE MONITORING TESTS")
        self.log(f"Target System: {self.base_url}")
        self.log(f"Test Start Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        
        try:
            # Run all test categories
            self.test_health_monitoring()
            self.test_graph_monitoring() 
            self.test_admin_monitoring()
            self.test_service_endpoints()
            self.test_forced_health_check()
            self.test_journey_endpoints()
            self.test_concurrent_monitoring()
            
            # Generate final report
            self.generate_report()
            
        except KeyboardInterrupt:
            self.log("\n❌ Testing interrupted by user", "WARN")
        except Exception as e:
            self.log(f"\n💥 Unexpected error during testing: {e}", "ERROR")
        
        self.log(f"\n✅ Testing completed at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


def main():
    """Main entry point"""
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    
    print("=" * 80)
    print("🔍 TRANSPORT SYSTEM MONITORING TEST SUITE")
    print("=" * 80)
    print(f"Testing system at: {base_url}")
    print("This will test all monitoring endpoints and capabilities.")
    print("=" * 80)
    
    monitor = TransportSystemMonitor(base_url)
    monitor.run_full_test_suite()


if __name__ == "__main__":
    main()