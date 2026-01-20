package com.company.training.repository;

import com.company.training.entity.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    List<PdfDocument> findByStatus(PdfDocument.ProcessingStatus status);
    List<PdfDocument> findByType(PdfDocument.PdfType type);
    List<PdfDocument> findAllByOrderByUploadedAtDesc();
}