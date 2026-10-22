package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/calculate")
public class CalculatorServlet extends HttpServlet {
    private final Calculator calculator = new Calculator();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String operation = request.getParameter("operation");
        String firstValue = request.getParameter("a");
        String secondValue = request.getParameter("b");

        if (operation == null || firstValue == null || secondValue == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Please provide both numbers and an operation.");
            return;
        }

        final double result;
        try {
            int a = Integer.parseInt(firstValue);
            int b = Integer.parseInt(secondValue);
            result = switch (operation) {
                case "add" -> calculator.add(a, b);
                case "subtract" -> calculator.subtract(a, b);
                case "multiply" -> calculator.multiply(a, b);
                case "divide" -> calculator.divide(a, b);
                default -> throw new IllegalArgumentException("Invalid operation");
            };
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Both inputs must be valid integers.");
            return;
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\"><head><meta charset=\"UTF-8\">");
            out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            out.println("<title>Calculator Result</title></head><body>");
            out.println("<h1>Calculator Result</h1>");
            out.println("<p>Result: " + result + "</p>");
            out.println("<p><a href=\"index.html\">Back to Calculator</a></p>");
            out.println("</body></html>");
        }
    }
}
