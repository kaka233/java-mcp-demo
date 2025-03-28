import java.util.ArrayList;
import java.util.List;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.*;
import reactor.core.publisher.Mono;

/**
 * a stdio server demo, use StdioServerTransportProvider in sdk 0.8.1, 0.8.0 has a bug
 * build jar and command in corsor is  java -Dfile.encoding=UTF-8 -jar D:\\data\\mcp.jar
 */
public class Main {
    
    public static void main(String[] args) {
        ServerCapabilities capabilities = ServerCapabilities.builder()
                                                            .logging()
                                                            .prompts(false)
                                                            .resources(false, false)
                                                            .tools(true)
                                                            .build();
        
        StdioServerTransportProvider provider = new StdioServerTransportProvider();
        
        McpAsyncServer asyncServer = McpServer.async(provider)
                                              .serverInfo("stdio", "1.0.0")
                                              .capabilities(capabilities)
                                              .tools(getSyncToolSpecification())
                                              .build();
        
        asyncServer.loggingNotification(LoggingMessageNotification.builder()
                                                                  .level(LoggingLevel.INFO)
                                                                  .logger("custom-logger")
                                                                  .data("Server initialized")
                                                                  .build());
    }
    
    private static AsyncToolSpecification getSyncToolSpecification() {
        String calculatorSchema = """
                                  {
                                  	"additionalProperties": true,
                                  	"type": "object",
                                  	"required":["operation", "left" , "right"],
                                  	"properties": {"operation":{"type":"string"}, "left":{"type":"number"}, "right":{"type":"number"} }
                                  }
                                  """;
        Tool calculatorTool = new Tool("diyCalculator", "a calculator", calculatorSchema);
        return new AsyncToolSpecification(
                calculatorTool,
                (exchange, arguments) -> {
                    List<Content> content   = new ArrayList<>();
                    String        operation = (String) arguments.get("operation");
                    Number        left      = (Number) arguments.get("left");
                    Number        right     = (Number) arguments.get("right");
                    content.add(new TextContent(parseExpression(operation, left.doubleValue(), right.doubleValue())));
                    return Mono.just(new CallToolResult(content, false));
                }
        );
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
