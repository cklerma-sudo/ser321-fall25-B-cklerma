package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import org.json.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class LibraryImpl extends LibraryGrpc.LibraryImplBase {
    private final JSONArray catalog;

    public LibraryImpl() {
        File JSONfile = new File("library_data.json");

        if (JSONfile.exists()) {
            try {
                String books = Files.readString(Paths.get("library_data.json"));
                catalog = new JSONArray(books);
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize library", e);
            }
        }
        else {
            try {
                String books = Files.readString(Paths.get("books.txt"));
                catalog = new JSONArray(books);
                for (int i = 0; i < catalog.length(); i++) {
                    JSONObject book = catalog.getJSONObject(i);
                    book.put("is_borrowed", false);
                    book.put("borrowed_by", "");
                    book.put("return_by", "");
                }
                Files.writeString(Paths.get("library_data.json"), catalog.toString(2));
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize library", e);
            }
        }
    }

    @Override
    public void listBooks(com.google.protobuf.Empty req, StreamObserver<BookListResponse> responseObserver){
        if (catalog.length() == 0) {
            responseObserver.onNext(buildBookListErrorResponse("There are no books in the library yet!"));
            responseObserver.onCompleted();
            return;
        }
        BookListResponse.Builder repBuilder = BookListResponse.newBuilder();

        for (int i = 0; i < catalog.length(); i++) {
            JSONObject JSONbook = catalog.getJSONObject(i);
            Book serviceBook = Book.newBuilder()
                    .setTitle(JSONbook.getString("title"))
                    .setAuthor(JSONbook.getString("author"))
                    .setIsbn(JSONbook.getString("isbn"))
                    .setIsBorrowed(JSONbook.getBoolean("is_borrowed"))
                    .setReturnBy(JSONbook.getString("return_by"))
                    .setBorrowedBy(JSONbook.getString("borrowed_by"))
                    .build();
            repBuilder.addBooks(serviceBook);
        }
        repBuilder.setIsSuccess(true);
        responseObserver.onNext(repBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void searchBooks(BookSearchRequest req, StreamObserver<BookListResponse> responseObserver){
        if (req.getQuery().isEmpty()) {
            responseObserver.onNext(buildBookListErrorResponse("The query is empty!"));
            responseObserver.onCompleted();
            return;
        }
        BookListResponse.Builder repBuilder = BookListResponse.newBuilder();
        boolean found = false;
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject JSONbook = catalog.getJSONObject(i);
            if (JSONbook.getString("title").equalsIgnoreCase(req.getQuery())) {
                found = true;
                Book serviceBook = Book.newBuilder()
                        .setTitle(JSONbook.getString("title"))
                        .setAuthor(JSONbook.getString("author"))
                        .setIsbn(JSONbook.getString("isbn"))
                        .setIsBorrowed(JSONbook.getBoolean("is_borrowed"))
                        .setReturnBy(JSONbook.getString("return_by"))
                        .setBorrowedBy(JSONbook.getString("borrowed_by"))
                        .build();
                repBuilder.addBooks(serviceBook);
            }
            else if (JSONbook.getString("author").equalsIgnoreCase(req.getQuery())) {
                found = true;
                Book serviceBook = Book.newBuilder()
                        .setTitle(JSONbook.getString("title"))
                        .setAuthor(JSONbook.getString("author"))
                        .setIsbn(JSONbook.getString("isbn"))
                        .setIsBorrowed(JSONbook.getBoolean("is_borrowed"))
                        .setReturnBy(JSONbook.getString("return_by"))
                        .setBorrowedBy(JSONbook.getString("borrowed_by"))
                        .build();
                repBuilder.addBooks(serviceBook);
            }
        }
        if (found) {
            repBuilder.setIsSuccess(true);
            responseObserver.onNext(repBuilder.build());
            responseObserver.onCompleted();
        }
        else {
            responseObserver.onNext(buildBookListErrorResponse("The book was not found"));
            responseObserver.onCompleted();
        }
    }

    @Override
    public void borrowBook(BorrowRequest req, StreamObserver<BorrowResponse> responseObserver){
        if (req.getIsbn().isEmpty() || req.getBorrowerName().isEmpty() || req.getBorrowDate().isEmpty()) {
            responseObserver.onNext(buildBorrowErrorResponse("Missing Name, ISBN, or borrowed date field"));
            responseObserver.onCompleted();
            return;
        }
        if (!req.getBorrowDate().matches("^\\d{4}-\\d{2}-\\d{2}$")){
            responseObserver.onNext(buildBorrowErrorResponse("Invalid date format, must be YYYY-MM-DD"));
            responseObserver.onCompleted();
            return;
        }
        boolean found = false;
        String returnDate = "";
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject JSONbook = catalog.getJSONObject(i);
            if (JSONbook.getString("isbn").equals(req.getIsbn())) {
                if (JSONbook.getBoolean("is_borrowed")) {
                    responseObserver.onNext(buildBorrowErrorResponse("The book is already borrowed"));
                    responseObserver.onCompleted();
                    return;
                }
                found = true;
                returnDate = addOneMonth(req.getBorrowDate());
                JSONbook.put("return_by", returnDate);
                JSONbook.put("borrowed_by", req.getBorrowerName());
                JSONbook.put("is_borrowed", true);
                saveCatalog();
                break;
            }
        }
        if (found) {
            BorrowResponse rep = BorrowResponse.newBuilder()
                    .setIsSuccess(true)
                    .setMessage("Successfully Borrowed! Please return by " + returnDate)
                    .build();
            responseObserver.onNext(rep);
            responseObserver.onCompleted();
        }
        else {
            responseObserver.onNext(buildBorrowErrorResponse("Invalid ISBN the book was not found"));
            responseObserver.onCompleted();
        }
    }

    @Override
    public void returnBook(ReturnRequest req, StreamObserver<ReturnResponse> responseObserver){
        if (req.getIsbn().isEmpty() || req.getReturnDate().isEmpty()) {
            responseObserver.onNext(buildReturnErrorResponse("Missing ISBN or return date field"));
            responseObserver.onCompleted();
            return;
        }
        if (!req.getReturnDate().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            responseObserver.onNext(buildReturnErrorResponse("Invalid date format, must be YYYY-MM-DD"));
            responseObserver.onCompleted();
            return;
        }
        boolean found = false, onTime = false;
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject JSONbook = catalog.getJSONObject(i);
            if (JSONbook.getString("isbn").equals(req.getIsbn())) {
                found = true;
                if (!JSONbook.getBoolean("is_borrowed")) {
                    responseObserver.onNext(buildReturnErrorResponse("This book has not borrowed yet"));
                    responseObserver.onCompleted();
                    return;
                }
                onTime = isOnTime(req.getReturnDate(), JSONbook.getString("return_by"));
                JSONbook.put("return_by", "");
                JSONbook.put("borrowed_by", "");
                JSONbook.put("is_borrowed", false);
                saveCatalog();
                break;
            }
        }
        if (!found) {
            responseObserver.onNext(buildReturnErrorResponse("The book was not found"));
            responseObserver.onCompleted();
            return;
        }
        if (!onTime) {
            ReturnResponse rep = ReturnResponse.newBuilder()
                    .setIsSuccess(true)
                    .setMessage("Successfully Returned! But you were late")
                    .build();
            responseObserver.onNext(rep);
            responseObserver.onCompleted();
        }
        else {
            ReturnResponse rep = ReturnResponse.newBuilder()
                    .setIsSuccess(true)
                    .setMessage("Successfully Returned! You were on time")
                    .build();
            responseObserver.onNext(rep);
            responseObserver.onCompleted();
        }
    }

    private BookListResponse buildBookListErrorResponse(String error) {
        BookListResponse.Builder resBuilder = BookListResponse.newBuilder();
        resBuilder.setError(error).setIsSuccess(false);
        return resBuilder.build();
    }

    private BorrowResponse buildBorrowErrorResponse(String error) {
        BorrowResponse.Builder resBuilder = BorrowResponse.newBuilder();
        resBuilder.setError(error).setIsSuccess(false);
        return resBuilder.build();
    }

    private ReturnResponse buildReturnErrorResponse(String error) {
        ReturnResponse.Builder resBuilder = ReturnResponse.newBuilder();
        resBuilder.setError(error).setIsSuccess(false);
        return resBuilder.build();
    }

    private String addOneMonth(String date) {
        String yearString = date.substring(0, 4);
        String monthString = date.substring(5, 7);
        String dayString = date.substring(8, 10);

        int year = Integer.parseInt(yearString);
        int month = Integer.parseInt(monthString);

        month = month + 1;
        if (month > 12) {
            month = 1;
            year = year + 1;
        }
        String newMonth = String.format("%02d", month);
        String newYear = String.format("%04d", year);
        return newYear + "-" + newMonth + "-" + dayString;
    }

    private void saveCatalog() {
        try {
            Files.writeString(Paths.get("library_data.json"), catalog.toString(2));
        } catch (Exception e) {
            throw new RuntimeException("Failed to Save library", e);
        }
    }

    private boolean isOnTime(String returnDate, String deadlineDate) {
        return returnDate.compareTo(deadlineDate) <= 0;
    }

}
