package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import java.util.Set;

public class ConverterImpl extends ConverterGrpc.ConverterImplBase {
    private static final Set<String> supportedUnits = Set.of("KILOMETER", "MILE", "YARD", "FOOT", "KILOGRAM", "POUND", "CELSIUS", "FAHRENHEIT");
    private static final Set<String> lengthUnits = Set.of("KILOMETER", "MILE", "YARD", "FOOT");
    private static final Set<String> weightUnits = Set.of("KILOGRAM", "POUND");
    private static final Set<String> temperatureUnits = Set.of("CELSIUS", "FAHRENHEIT");

    @Override
    public void convert(ConversionRequest req, StreamObserver<ConversionResponse> responseObserver) {
        String fromUnit = req.getFromUnit().toUpperCase().trim();
        String toUnit = req.getToUnit().toUpperCase().trim();

        ConversionResponse.Builder resBuilder = ConversionResponse.newBuilder();
        if (!req.hasValue() || fromUnit.isEmpty() || toUnit.isEmpty()) {
            responseObserver.onNext(buildErrorResponse("Conversion value or the units were not given. All fields are required."));
            responseObserver.onCompleted();
            return;
        }
        double value = req.getValue();
        if (!supportedUnits.contains(fromUnit) || !supportedUnits.contains(toUnit)) {
            responseObserver.onNext(buildErrorResponse("Unsupported unit given. Units must be spelt exactly with no abbreviation. Supported units are: " + supportedUnits));
            responseObserver.onCompleted();
            return;
        }
        if (fromUnit.equals("CELSIUS") && value < -273.15) {
            responseObserver.onNext(buildErrorResponse("You are below absolute zero!"));
            responseObserver.onCompleted();
            return;
        }
        if (fromUnit.equals("FAHRENHEIT") && value < -459.67) {
            responseObserver.onNext(buildErrorResponse("You are below absolute zero!"));
            responseObserver.onCompleted();
            return;
        }
        if (fromUnit.equals(toUnit)) {
            responseObserver.onNext(buildErrorResponse("They are the same unit"));
            responseObserver.onCompleted();
            return;
        }
        if (lengthUnits.contains(fromUnit) && !lengthUnits.contains(toUnit)) {
            responseObserver.onNext(buildErrorResponse("Invalid conversion request, both units must be length units"));
            responseObserver.onCompleted();
            return;
        }
        if (weightUnits.contains(fromUnit) && !weightUnits.contains(toUnit)) {
            responseObserver.onNext(buildErrorResponse("Invalid conversion request, both units must be weight units"));
            responseObserver.onCompleted();
            return;
        }
        if (temperatureUnits.contains(fromUnit) && !temperatureUnits.contains(toUnit)) {
            responseObserver.onNext(buildErrorResponse("Invalid conversion request, both units must be temperature units"));
            responseObserver.onCompleted();
            return;
        }

        if (lengthUnits.contains(fromUnit)) value = lengthConversion(fromUnit, toUnit, value);
        else if (weightUnits.contains(fromUnit)) value = weightConversion(fromUnit, value);
        else if (temperatureUnits.contains(fromUnit)) value = temperatureConversion(fromUnit, value);
        else {
            responseObserver.onNext(buildErrorResponse("Something unexpected happened, please try again."));
            responseObserver.onCompleted();
            return;
        }

        resBuilder.setIsSuccess(true).setResult(value);
        responseObserver.onNext(resBuilder.build());
        responseObserver.onCompleted();
    }

    private ConversionResponse buildErrorResponse(String error) {
        ConversionResponse.Builder resBuilder = ConversionResponse.newBuilder();
        resBuilder.setError(error).setIsSuccess(false);
        return resBuilder.build();
    }

    private double lengthConversion(String from, String to, double value) {
        if (from.equals("KILOMETER")) {
            if (to.equals("MILE")) value = value * 0.621371;
            if (to.equals("YARD")) value = (value * 0.621371) * 1760.0;
            if (to.equals("FOOT")) value = (value * 0.621371) * 1760.0 * 3.0;
        }
        if (from.equals("MILE")) {
            if (to.equals("YARD")) value = value * 1760.0;
            if (to.equals("FOOT")) value = value * 1760.0 * 3.0;
            if (to.equals("KILOMETER")) value = value * (1.0 / 0.621371);
        }
        if (from.equals("YARD")) {
            if (to.equals("FOOT")) value = value * 3.0;
            if (to.equals("KILOMETER")) value = (value / 1760.0) * (1.0 / 0.621371);
            if (to.equals("MILE")) value = value / 1760.0;
        }
        if (from.equals("FOOT")) {
            if (to.equals("YARD")) value = value / 3.0;
            if (to.equals("KILOMETER")) value = ((value / 3.0) / 1760.0) * (1.0 / 0.621371);
            if (to.equals("MILE")) value = (value / 3.0) / 1760.0;
        }

        return value;
    }

    private double weightConversion(String from, double value) {
        if (from.equals("KILOGRAM")) value = value * 2.20462;
        if (from.equals("POUND")) value = value / 2.20462;
        return value;
    }

    private double temperatureConversion(String from, double value) {
        if (from.equals("CELSIUS")) value = (value * (9.0 / 5.0)) + 32.0;
        if (from.equals("FAHRENHEIT")) value = (value - 32.0) * (5.0 / 9.0);
        return value;
    }

}
