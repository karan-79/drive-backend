package com.project.mydrive.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "DIRECTORIES")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    boolean isStarred = false;

    @Column(name = "IS_DELETED")
    boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_DIR")
    Directory parentDirectory;

    @OneToMany(mappedBy = "parentDirectory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<File> files;

    @OneToMany(mappedBy = "parentDirectory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Directory> subDirectories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_ID", nullable = false)
    User owner;
}
