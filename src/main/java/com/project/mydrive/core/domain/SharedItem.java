package com.project.mydrive.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHARED_ITEMS")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SharedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_ID")
    File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIRECTORY_ID")
    Directory directory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    User user;

    ItemType itemType;

}
