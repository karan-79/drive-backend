package com.project.mydrive.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "RECENT_ITEMS")
@Getter
@Setter
public class RecentItems {

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