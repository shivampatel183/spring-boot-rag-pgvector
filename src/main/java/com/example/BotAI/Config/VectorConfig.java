package com.example.BotAI.Config;

import com.example.BotAI.Entity.StudentEntity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VectorConfig {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public VectorConfig(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    // =========================
    // ADD
    // =========================

    public void addStudentVector(StudentEntity student) {

        String studentText = """
                Student ID: %s
                Name: %s
                School: %s
                Email: %s
                Phone: %s
                Description: %s
                """.formatted(
                student.getId(),
                student.getName(),
                student.getSchool(),
                student.getEmail(),
                student.getPhone(),
                student.getDescription()
        );

        Map<String, Object> metadata = Map.of(
                "studentId", student.getId(),
                "name", student.getName(),
                "school", student.getSchool()
        );

        Document document = new Document(
                studentText,
                metadata
        );

        vectorStore.add(List.of(document));
    }


    // =========================
    // UPDATE
    // =========================

    public void updateStudentVector(StudentEntity student) {

        deleteStudentVector(student);

        addStudentVector(student);
    }


    // =========================
    // DELETE
    // =========================

    public void deleteStudentVector(StudentEntity student) {

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(student.getName())
                        .topK(20)
                        .filterExpression(
                                "studentId == " + student.getId()
                        )
                        .build()
        );

        if (documents.isEmpty()) {
            return;
        }

        List<String> documentIds = documents.stream()
                .map(Document::getId)
                .toList();

        vectorStore.delete(documentIds);
    }


    // =========================
    // ASK AI
    // =========================

    public String askAI(String question) {

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(10)
                        .similarityThreshold(0.4)
                        .build()
        );

        if (documents.isEmpty()) {

            return "I don't know the answer based on the available student data.";
        }

        StringBuilder contextBuilder = new StringBuilder();

        for (Document document : documents) {

            contextBuilder
                    .append(document.getText())
                    .append("\n\n");
        }

        String context = contextBuilder.toString();

        String prompt = """
                You are an AI assistant for a student management application.

                Answer the user's question using ONLY the information
                provided in the context below.

                Rules:
                1. Do not invent information.
                2. Do not make assumptions.
                3. If the answer is not present in the context,
                   say "I don't know".
                4. Give a clear and natural human-like answer.
                5. If multiple students match the question,
                   mention all relevant students.
                6. Use exact values for names, emails, phones,
                   schools and IDs.

                Context:
                --------------------
                %s
                --------------------

                User Question:
                %s
                """.formatted(
                context,
                question
        );

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}