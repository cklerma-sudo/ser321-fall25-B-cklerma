package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;

import java.util.Set;

public class TriangleImpl extends TriangleGrpc.TriangleImplBase {
    private static final Set<String> styles = Set.of("RIGHT", "LEFT", "CENTER", "HOLLOW");

    @Override
    public void printTriangle(printRequest req, StreamObserver<printResponse> responseObserver) {
        if (req.getHeight() <= 0 || req.getStyle().isEmpty() ) {
            responseObserver.onNext(buildErrorResponse("Height must be greater than 0 and style must be given"));
            responseObserver.onCompleted();
            return;
        }
        if (!styles.contains(req.getStyle().toUpperCase().trim())) {
            responseObserver.onNext(buildErrorResponse("Style must be one of these: " + styles));
            responseObserver.onCompleted();
            return;
        }
        if (req.getCh().trim().length() != 1) {
            responseObserver.onNext(buildErrorResponse("The character must be a single character"));
            responseObserver.onCompleted();
            return;
        }
        String style = req.getStyle().toUpperCase().trim();
        int size = req.getHeight();
        String ch = req.getCh().trim();
        String triangle;
        if (style.equals("RIGHT")) {
            triangle = rightTriangle(ch, size);
        }
        else if (style.equals("LEFT")) {
            triangle = leftTriangle(ch, size);
        }
        else if (style.equals("CENTER")) {
            triangle = centerTriangle(ch, size);
        }
        else {
            triangle = hollowTriangle(ch, size);
        }
        printResponse rep = printResponse.newBuilder()
                .setIsSuccess(true)
                .setTriangle(triangle)
                .build();
        responseObserver.onNext(rep);
        responseObserver.onCompleted();
    }

    @Override
    public void calcArea(calcRequest req, StreamObserver<calcResponse> responseObserver) {
        if (req.getSize() <= 0){
            calcResponse errorRep = calcResponse.newBuilder()
                    .setError("Size is required and must be greater than 0")
                    .setIsSuccess(false)
                    .build();
            responseObserver.onNext(errorRep);
            responseObserver.onCompleted();
            return;
        }
        int area = 0;
        for (int i = 1; i <= req.getSize(); i++) {
            area += i;
        }
        calcResponse rep = calcResponse.newBuilder()
                .setArea(area)
                .setIsSuccess(true)
                .build();
        responseObserver.onNext(rep);
        responseObserver.onCompleted();
    }

    private printResponse buildErrorResponse(String error) {
        printResponse.Builder resBuilder = printResponse.newBuilder();
        resBuilder.setError(error).setIsSuccess(false);
        return resBuilder.build();
    }

    private String leftTriangle(String ch, int size) {
        StringBuilder triangle = new StringBuilder();
        for (int i = 1; i <= size; i++) {
            triangle.append(ch.repeat(i));
            if (i < size) triangle.append("\n");
        }
        return triangle.toString();
    }

    private String rightTriangle(String ch, int size) {
        StringBuilder triangle = new StringBuilder();
        for (int i = 1; i <= size; i++) {
            int spaces = size - i;
            triangle.append(" ".repeat(spaces));
            triangle.append(ch.repeat(i));
            if (i < size) triangle.append("\n");
        }
        return triangle.toString();
    }

    private String centerTriangle(String ch, int size) {
        StringBuilder triangle = new StringBuilder();
        for (int i = 1; i <= size; i++) {
            int spaces = size - i;
            int chars = 2 * i - 1;
            triangle.append(" ".repeat(spaces));
            triangle.append(ch.repeat(chars));
            if (i < size) triangle.append("\n");
        }
        return triangle.toString();}

    private String hollowTriangle(String ch, int size) {
        StringBuilder triangle = new StringBuilder();
        for (int i = 1; i <= size; i++) {
            triangle.append(" ".repeat(size - i));
            if (i == 1) triangle.append(ch);
            else if (i == size) {
                int base = 2 * i - 1;
                triangle.append(ch.repeat(base));
            }
            else {
                triangle.append(ch);
                triangle.append(" ".repeat(2 * i - 3));
                triangle.append(ch);
            }

            if (i < size) triangle.append("\n");
        }
        return triangle.toString();
    }
}
