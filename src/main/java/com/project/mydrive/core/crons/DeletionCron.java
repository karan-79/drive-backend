package com.project.mydrive.core.crons;

import com.project.mydrive.core.service.CleanUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeletionCron {

    private final CleanUpService cleanUpService;
    @Scheduled(cron = "0 0 0 * * *")
    void run() {
        System.out.println("---------Deletion Cron started------");
        cleanUpService.deleteFilesOnBlobAsync();
        System.out.println("---------Deletion Cron finished------");
    }

}
