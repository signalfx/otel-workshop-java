# Java Http Server with OpenTelemetry instrumentation

This app listens on port `3000` and exposes a single endpoint at `/` that
responds with the string "hello from java\n this is the end of the journey...
for today".

The OpenTelemetry Java HTTP Example was used as a reference for this sample.
https://github.com/open-telemetry/opentelemetry-java/tree/master/examples/http

## Running the application

The application is available at
https://glitch.com/edit/#!/signalfx-otel-workshop-java. By default, it runs an
uninstrumented version of the application. From the Glitch site, you should
select the name of the Glitch project (top left) and select `Remix Project`.
You will now have a new Glitch project. The name of the project is listed in
the top left of the window.

To run this workshop locally, you'll need Java and Make to be able to run
the service. Install the prerequisites by running `make install`. Next, run
`make run` and then go to http://localhost:3000 to access the app.


## Instrumenting Java HTTP server and client with OpenTelemetry

Your task is to instrument this application using [OpenTelemetry
Java](https://github.com/open-telemetry/opentelemetry-java). If you get
stuck, check out the `src_instrumented` directory.

### 1. Add the relevant dependencies and repositories to pom.xml

```diff
    <!-- library dependencies -->
    <dependencies>
        <dependency>
            <groupId>io.github.cdimascio</groupId>
            <artifactId>java-dotenv</artifactId>
            <version>5.1.3</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-context</artifactId>
            <version>1.0.1</version>
        </dependency>
-    </dependencies>
+      <dependency>
+        <groupId>io.opentelemetry</groupId>
+        <artifactId>opentelemetry-sdk</artifactId>
+        <version>0.4.0-SNAPSHOT</version>
+      </dependency>
+      <dependency>
+        <groupId>io.opentelemetry</groupId>
+        <artifactId>opentelemetry-exporters-logging</artifactId>
+        <version>0.4.0-SNAPSHOT</version>
+      </dependency>
+    </dependencies>
+    <repositories>
+      <repository>
+        <id>oss.sonatype.org-snapshot</id>
+        <url>https://oss.jfrog.org/artifactory/oss-snapshot-local</url>
+      </repository>
+    </repositories>
```

## 2. Import the packages required for instrumenting your app

```diff
+import io.opentelemetry.OpenTelemetry;
+import io.opentelemetry.common.AttributeValue;
+import io.opentelemetry.context.ContextUtils;
+import io.opentelemetry.context.Scope;
+import io.opentelemetry.context.propagation.HttpTextFormat;
+import io.opentelemetry.exporters.logging.LoggingSpanExporter;
+import io.opentelemetry.sdk.OpenTelemetrySdk;
+import io.opentelemetry.sdk.trace.TracerSdkProvider;
+import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
+import io.opentelemetry.trace.*;

```

Note: The recommended deployment model for OpenTelemetry is to have
applications export in OpenTelemetry (OTLP) format to the OpenTelemetry
Collector and have the OpenTelemetry Collector send to your back-end(s) of
choice. OTLP uses gRPC and unfortunately it does not appear Glitch supports
gRPC. For this workshop, all other languages are exporting in Zipkin and it
isn't supported in Java yet. All traces emitted by this application will be
logged instead of forwarded to the OpenTelemetry Collector.

## 3. Initiate the tracer, the logging exporter and invoke it during the Main class initializer.

```diff

+  // OTel API
+  private static Tracer tracer =
+      OpenTelemetry.getTracerProvider().get("Main");
+  // Export traces to log.
+  private static LoggingSpanExporter loggingExporter = new LoggingSpanExporter();

+  private void initTracer() {
+    // Get the tracer
+    TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();
+    // Show that multiple exporters can be used

+    // Set to export the traces also to a log file.
+    tracerProvider.addSpanProcessor(SimpleSpansProcessor.newBuilder(loggingExporter).build());
+  }
```

```diff
private Main(int port) throws IOException {
+    initTracer();

```

## 4. Manually instrument the HTTP Handler

```diff
    @Override
    public void handle(HttpExchange he) throws IOException {
+      Span span = tracer.spanBuilder("java app").setSpanKind(Span.Kind.SERVER);
+      span.setAttribute("uri", he.getRequestURI().toString());

       // Process the request
      String response = "hello from java\n this is the end of the journey... for today";
      he.sendResponseHeaders(200, response.length());
      OutputStream os = he.getResponseBody();
      os.write(response.getBytes(Charset.defaultCharset()));
      os.close();
      System.out.println("Served Client: " + he.getRemoteAddress());

+      span.setAttribute("response", response);

+      // Everything works fine in this example
+      span.setStatus(Status.OK);

+      // Close the span
+      span.end();
    }
```

This will generate spans with the attributes `uri` and `response`.
