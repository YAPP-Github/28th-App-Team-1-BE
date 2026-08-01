package com.yapp.d14.consent.adapter.out.persistence.entity;

import com.yapp.d14.consent.domain.ConsentDocument;
import com.yapp.d14.consent.domain.ConsentItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "consent_documents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item", "version"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentDocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentItem item;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public static ConsentDocumentJpaEntity from(ConsentDocument consentDocument) {
        ConsentDocumentJpaEntity entity = new ConsentDocumentJpaEntity();
        entity.item = consentDocument.getItem();
        entity.version = consentDocument.getVersion();
        entity.title = consentDocument.getTitle();
        entity.content = consentDocument.getContent();
        return entity;
    }

    public ConsentDocument toDomain() {
        return ConsentDocument.of(item, version, title, content);
    }
}
