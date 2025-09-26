```mermaid
graph TB
    %% Core Domain Classes
    StopNode[Stop<br/>Abstract base class<br/>- name, coordinates<br/>- trips list]
    BusStopNode[BusStop<br/>extends Stop<br/>- routeCodes<br/>- company]
    TripNode[Trip<br/>- departure/destination stops<br/>- duration, dayType<br/>- mode, departureTime]
    JourneyNode[Journey<br/>- List of trips<br/>- total duration]

    %% Graph Classes
    GraphNode[Graph<br/>Unified multimodal graph<br/>- combines all transport modes<br/>- RAPTOR routing]
    BusGraphNode[BusGraph<br/>Bus-only graph<br/>- MyCiTi + Golden Arrow<br/>- RAPTOR + CSA routing]

    %% Transport-specific Graphs
    TaxiGraphNode[TaxiGraph<br/>Referenced via Graph]
    TrainGraphNode[TrainGraph<br/>Referenced via Graph]
    MyCitiBusGraphNode[MyCitiBusGraph<br/>Referenced via Graph/BusGraph]
    GABusGraphNode[GABusGraph<br/>Referenced via Graph/BusGraph]

    %% Routing Components
    RaptorRouterNode[RaptorRouter<br/>BusGraph.RaptorRouter<br/>RAPTOR algorithm implementation]
    ConnectionScanRouterNode[ConnectionScanRouter<br/>BusGraph.ConnectionScanRouter<br/>Connection Scan algorithm]
    RaptorNode[Raptor<br/>Graph.Raptor<br/>Inner class for unified routing]

    %% Spring Configuration
    GraphConfigNode[GraphConfig<br/>@Configuration<br/>Spring bean setup]
    WebConfigNode[WebConfig<br/>@Configuration<br/>CORS + Interceptors]

    %% Monitoring & Utilities
    SystemLogNode[SystemLog<br/>Static utility<br/>- endpoint tracking<br/>- stop/route logging]
    SystemHealthMonitorNode[SystemHealthMonitor<br/>@Service<br/>Health checking]
    PerformanceMetricsRegistryNode[PerformanceMetricsRegistry<br/>Static utility<br/>Performance tracking]
    RequestTimingInterceptorNode[RequestTimingInterceptor<br/>@Component<br/>Request timing]
    MetricsResponseBuilderNode[MetricsResponseBuilder<br/>Utility for metrics responses]

    %% Main Application
    AppNode[PublicTransportationJourneyPlannerApplication<br/>@SpringBootApplication<br/>Main entry point]

    %% Test/Utility Classes
    TestBusGraphsNode[TestBusGraphs<br/>Main method test]
    RaptorDiagnosticsNode[RaptorDiagnostics<br/>Diagnostic utility]
    GeoJsonReaderNode[GeoJsonReader<br/>GeoJSON parsing utility]
    GeoServiceNode[GeoService<br/>Place -> coordinates lookup]
    TripParseExampleNode[TripParseExample<br/>Trip string parser]
    MyFileLoaderNode[MyFileLoader<br/>Resource file loader]
    DataFilesRegistryNode[DataFilesRegistry<br/>File path registry]
    TimeNode[Time<br/>Time handling utility]

    %% Controller Layer
    AdminControllerNode[AdminController<br/>@RestController<br/>System admin operations]
    GraphControllerNode[GraphController<br/>@RestController<br/>Unified multimodal API]
    TrainControllerNode[TrainController<br/>@RestController<br/>Train-specific API]
    MyCitiBusControllerNode[MyCitiBusController<br/>@RestController<br/>MyCiti bus API]
    GABusControllerNode[GABusController<br/>@RestController<br/>GA bus API]
    TaxiControllerNode[TaxiController<br/>@RestController<br/>Taxi API]
    SystemMonitoringControllerNode[SystemMonitoringController<br/>@RestController<br/>Health monitoring API]

    %% Specialized Transport-Specific Classes
    RouteCoordinateExtractorNode[RouteCoordinateExtractor<br/>Extracts railway coordinates<br/>from GeoJSON data]
    TrainJourneyNode[TrainJourney<br/>Train-specific journey result]
    TrainStopNode[TrainStop<br/>extends Stop<br/>Train stops with rail data]
    TrainTripsNode[TrainTrips<br/>extends Trip<br/>Train trips with schedules]

    MyCitiBusJourneyNode[MyCitiBusJourney<br/>MyCiti journey result]
    MyCitiStopNode[MyCitiStop<br/>extends BusStop<br/>MyCiti stops]
    MyCitiTripNode[MyCitiTrip<br/>extends Trip<br/>MyCiti trips]

    GABusJourneyNode[GABusJourney<br/>GA journey result]
    GAStopNode[GAStop<br/>extends BusStop<br/>Golden Arrow stops]
    GATripNode[GATrip<br/>extends Trip<br/>Golden Arrow trips]
    ReadInGAStopsNode[ReadInGAStops<br/>GA data loader utility]

    TaxiStopNode[TaxiStop<br/>extends Stop<br/>Taxi stops]
    TaxiTripNode[TaxiTrip<br/>extends Trip<br/>Taxi trips]

    %% Relationships
    BusStopNode -.->|extends| StopNode
    TrainStopNode -.->|extends| StopNode
    TaxiStopNode -.->|extends| StopNode
    MyCitiStopNode -.->|extends| BusStopNode
    GAStopNode -.->|extends| BusStopNode

    TripNode -->|has| StopNode
    TrainTripsNode -.->|extends| TripNode
    MyCitiTripNode -.->|extends| TripNode
    GATripNode -.->|extends| TripNode
    TaxiTripNode -.->|extends| TripNode
    JourneyNode -->|contains| TripNode

    TrainJourneyNode -.->|specializes| JourneyNode
    MyCitiBusJourneyNode -.->|specializes| JourneyNode
    GABusJourneyNode -.->|specializes| JourneyNode

    GraphNode -->|manages| StopNode
    GraphNode -->|manages| TripNode
    GraphNode -->|contains| RaptorNode
    GraphNode -->|references| TaxiGraphNode
    GraphNode -->|references| TrainGraphNode
    GraphNode -->|references| MyCitiBusGraphNode
    GraphNode -->|references| GABusGraphNode

    BusGraphNode -->|manages| StopNode
    BusGraphNode -->|manages| TripNode
    BusGraphNode -->|contains| RaptorRouterNode
    BusGraphNode -->|contains| ConnectionScanRouterNode
    BusGraphNode -->|references| MyCitiBusGraphNode
    BusGraphNode -->|references| GABusGraphNode

    TrainGraphNode -->|manages| TrainStopNode
    TrainGraphNode -->|manages| TrainTripsNode
    MyCitiBusGraphNode -->|manages| MyCitiStopNode
    MyCitiBusGraphNode -->|manages| MyCitiTripNode
    GABusGraphNode -->|manages| GAStopNode
    GABusGraphNode -->|manages| GATripNode
    TaxiGraphNode -->|manages| TaxiStopNode
    TaxiGraphNode -->|manages| TaxiTripNode

    TrainControllerNode -->|uses| TrainGraphNode
    TrainControllerNode -->|uses| RouteCoordinateExtractorNode
    MyCitiBusControllerNode -->|uses| MyCitiBusGraphNode
    GABusControllerNode -->|uses| GABusGraphNode
    TaxiControllerNode -->|uses| TaxiGraphNode
    GraphControllerNode -->|uses| GraphNode

    AdminControllerNode -->|coordinates| TrainControllerNode
    AdminControllerNode -->|coordinates| MyCitiBusControllerNode
    AdminControllerNode -->|coordinates| TaxiControllerNode
    SystemMonitoringControllerNode -->|uses| SystemHealthMonitorNode

    GraphConfigNode -->|creates @Bean| GraphNode
    GraphConfigNode -->|creates @Bean| TaxiGraphNode
    GraphConfigNode -->|creates @Bean| TrainGraphNode
    GraphConfigNode -->|creates @Bean| MyCitiBusGraphNode
    GraphConfigNode -->|creates @Bean| GABusGraphNode

    WebConfigNode -->|configures| RequestTimingInterceptorNode

    RequestTimingInterceptorNode -->|records to| PerformanceMetricsRegistryNode
    MetricsResponseBuilderNode -->|reads from| PerformanceMetricsRegistryNode

    SystemHealthMonitorNode -->|monitors| TrainGraphNode
    SystemHealthMonitorNode -->|monitors| MyCitiBusGraphNode
    SystemHealthMonitorNode -->|monitors| TaxiGraphNode
    SystemHealthMonitorNode -->|uses| PerformanceMetricsRegistryNode

    SystemLogNode -->|tracks| StopNode
    SystemLogNode -->|tracks endpoints| PerformanceMetricsRegistryNode

    MyFileLoaderNode -->|used by| TrainGraphNode
    MyFileLoaderNode -->|used by| MyCitiBusGraphNode
    MyFileLoaderNode -->|used by| GABusGraphNode
    MyFileLoaderNode -->|used by| TaxiGraphNode
    DataFilesRegistryNode -->|used by| MyFileLoaderNode

    RouteCoordinateExtractorNode -->|uses| MyFileLoaderNode
    RouteCoordinateExtractorNode -->|uses| DataFilesRegistryNode
    GeoServiceNode -->|provides| GeoJsonReaderNode
    TripParseExampleNode -->|used by| GABusGraphNode

    TestBusGraphsNode -->|tests| BusGraphNode
    RaptorDiagnosticsNode -->|tests| GraphNode
    RaptorDiagnosticsNode -->|tests| BusGraphNode

    AppNode -->|starts| GraphConfigNode

    %% Styling
    classDef coreClass fill:#e1f5fe
    classDef graphClass fill:#f3e5f5
    classDef configClass fill:#e8f5e8
    classDef monitorClass fill:#fff3e0
    classDef testClass fill:#fce4ec

    class StopNode,BusStopNode,TripNode,JourneyNode,TrainJourneyNode,MyCitiBusJourneyNode,GABusJourneyNode coreClass
    class GraphNode,BusGraphNode,TaxiGraphNode,TrainGraphNode,MyCitiBusGraphNode,GABusGraphNode,RaptorRouterNode,ConnectionScanRouterNode,RaptorNode graphClass
    class GraphConfigNode,WebConfigNode,AppNode configClass
    class SystemLogNode,SystemHealthMonitorNode,PerformanceMetricsRegistryNode,RequestTimingInterceptorNode,MetricsResponseBuilderNode monitorClass
    class TestBusGraphsNode,RaptorDiagnosticsNode testClass
```