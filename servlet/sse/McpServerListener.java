package me.fan.demo2.Listener;

import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;


@WebListener
public class McpServerListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        HttpServletSseServerTransportProvider provider = new HttpServletSseServerTransportProvider(
                new ObjectMapper(), "/messages"
        );
        ServerCapabilities capabilities = ServerCapabilities.builder()
                                                            .logging()
                                                            .prompts(false)
                                                            .resources(false, false)
                                                            .tools(true)
                                                            .build();
        SyncToolSpecification calculatorTool = getSyncToolSpecification();
        McpServer.sync(provider)
                 .serverInfo("sse", "1.0.0")
                 .capabilities(capabilities)
                 .tools(calculatorTool)
                 .build();
        
        ServletContext              context = sce.getServletContext();
        ServletRegistration.Dynamic servlet = context.addServlet("mcp", provider);
        servlet.addMapping("/*");
        servlet.setAsyncSupported(true);
    }
    
    private static SyncToolSpecification getSyncToolSpecification() {
        String calculatorSchema = """
                                  {
                                  	"additionalProperties": true,
                                  	"type": "object",
                                  	"required":["operation", "left" , "right"],
                                  	"properties": {"operation":{"type":"string"}, "left":{"type":"number"}, "right":{"type":"number"} }
                                  }
                                  """;
        Tool calculatorTool = new Tool("diyCalculator", "a calculator", calculatorSchema);
        return new SyncToolSpecification(
                calculatorTool,
                (exchange, arguments) -> {
                    List<Content> content   = new ArrayList<>();
                    String        operation = (String) arguments.get("operation");
                    Number        left      = (Number) arguments.get("left");
                    Number        right     = (Number) arguments.get("right");
                    content.add(new TextContent(parseExpression(operation, left.doubleValue(), right.doubleValue())));
                    return new CallToolResult(content, false);
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
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }
}
