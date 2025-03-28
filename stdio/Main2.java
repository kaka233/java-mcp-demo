import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;

/**
 * sdk 0.8.0 StdioServerTransportProvider has a bug: it work in windows terminal, but use cursor(or cherry stdio) after response "initialize", The program will crash with a NullPointerException，
 * java.lang.NullPointerException: Cannot invoke "io.modelcontextprotocol.spec.McpServerSession.handle(io.modelcontextprotocol.spec.McpSchema$JSONRPCMessage)" because "this.this$0.session" is null
 * i can't fix it, so i write this demo without session and reactor; sdk 0.8.1 fixed it. see the StdioServerTransportProvider demo in Main.java
 * build jar and command in corsor is java -Dfile.encoding=UTF-8 -jar D:\\data\\mcp.jar
 * cursor request info:
 * {"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":true,"prompts":false,"resources":true,"logging":false,"roots":{"listChanged":false}},"clientInfo":{"name":"cursor-vscode","version":"1.0.0"}},"jsonrpc":"2.0","id":0}
 * {"method":"notifications/initialized","jsonrpc":"2.0"}
 * {"method":"tools/list","jsonrpc":"2.0","id":1}
 * {"method":"tools/call","params":{"name":"diyCalculator","arguments":{"operation":"diy","left":3,"right":9}},"jsonrpc":"2.0","id":2}
 * 
*/
public class Main2 {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Main2.class);
    
    private static ListToolsResult listToolsResult = null;
    
    public static void main(String[] args) {
        listToolsResult = getToolsListRes();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String s = scanner.nextLine();
                // just for test
                if (s.equals("exit")) {
                    break;
                }
                if (s.isBlank()) {
                    continue;
                }
                try {
                    JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, s);
                    if (Objects.isNull(message)) {
                        continue;
                    }
                    if (message instanceof JSONRPCNotification notification) {
                        handleNotification(notification);
                        continue;
                    }
                    if (message instanceof JSONRPCRequest request) {
                        JSONRPCResponse jsonrpcResponse = handleRequest(request, objectMapper);
                        String          res             = objectMapper.writeValueAsString(jsonrpcResponse);
                        System.out.println(res);
                    }
                } catch (Exception e) {
                    logger.error("inner error", e);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("outer error", e);
        }
    }
    
    public static void handleNotification(JSONRPCNotification notification) {
        switch (notification.method()) {
            case McpSchema.METHOD_NOTIFICATION_INITIALIZED -> {
                // do nothing
                logger.debug("initialized");
            }
            case McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED -> {
                // do something
            }
            default -> {
                // do nothing
            }
        }
    }
    
    private static JSONRPCResponse handleRequest(JSONRPCRequest request, ObjectMapper objectMapper) {
        Object id     = request.id();
        String method = request.method();
        try {
            Object result = switch (method) {
                case McpSchema.METHOD_INITIALIZE -> handleInitialize();
                case McpSchema.METHOD_TOOLS_LIST -> listToolsResult;
                case McpSchema.METHOD_TOOLS_CALL -> {
                    CallToolRequest callRequest = objectMapper.convertValue(request.params(), CallToolRequest.class);
                    yield callTool(callRequest.name(), callRequest.arguments());
                }
                default -> null;
            };
            if (Objects.nonNull(result)) {
                return createSuccessResponse(id, result);
            }
            return null;
        } catch (Exception e) {
            logger.error("handleRequest", e);
        }
        return null;
    }
    
    private static JSONRPCResponse createSuccessResponse(Object id, Object result) {
        return new JSONRPCResponse(
                McpSchema.JSONRPC_VERSION,
                id,
                result,
                null
        );
    }
    
    public static InitializeResult handleInitialize() {
        ServerCapabilities capabilities = ServerCapabilities.builder()
                                                            .logging()
                                                            .prompts(true)
                                                            .resources(true, true)
                                                            .tools(true)
                                                            .build();
        Implementation serverInfo = new Implementation("MCP stdio Server", "1.0.0");
        return new InitializeResult(
                McpSchema.LATEST_PROTOCOL_VERSION,
                capabilities,
                serverInfo,
                "a stdio server demo"
        );
    }
    
    private static ListToolsResult getToolsListRes() {
        String calculatorSchema = """
                                  {
                                  	"additionalProperties": true,
                                  	"type": "object",
                                  	"required":["operation", "left" , "right"],
                                  	"properties": {"operation":{"type":"string"}, "left":{"type":"number"}, "right":{"type":"number"} }
                                  }
                                  """;
        Tool       calculatorTool = new Tool("diyCalculator", "a calculator", calculatorSchema);
        List<Tool> tools          = new ArrayList<>();
        tools.add(calculatorTool);
        return new ListToolsResult(tools, null);
    }
    
    public static CallToolResult callTool(String name, Map<String, Object> arguments) {
        List<Content> content = new ArrayList<>();
        switch (name) {
            case "echo" -> {
                String text = (String) arguments.get("text");
                content.add(new TextContent("myecho:" + text));
            }
            case "diyCalculator" -> {
                String operation = (String) arguments.get("operation");
                Number left      = (Number) arguments.get("left");
                Number right     = (Number) arguments.get("right");
                content.add(new TextContent(parseExpression(operation, left.doubleValue(), right.doubleValue())));
            }
            default -> throw new IllegalArgumentException("未知工具: " + name);
        }
        return new CallToolResult(content, false);
    }
    
    private static String parseExpression(String operation, double left, double right) {
        return switch (operation) {
            case "plus", "+" -> String.valueOf(left + right);
            case "minus", "-" -> String.valueOf(left - right);
            case "multi", "*", "x", "X" -> String.valueOf(left * right);
            case "divide", "/", "÷" -> String.valueOf(left / right);
            case "mod", "%" -> String.valueOf(left % right);
            case "diy" -> String.valueOf(left * 2 + right);
            default -> "250";
        };
    }
    
    
}
