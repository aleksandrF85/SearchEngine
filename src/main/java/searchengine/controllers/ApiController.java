package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.PageRecursiveAction;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    SitesList sitesList;
    private final StatisticsService statisticsService;
    @Autowired
    IndexingService indexingService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing(){
        if (indexingService.isIndexingInProgress()){

            return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация уже запущена"));
        }
        PageRecursiveAction.setStopIndexing(false);
        indexingService.startIndexingAll();

        return ResponseEntity.ok(new IndexingResponse(true, null));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing(){
        if (!indexingService.isIndexingInProgress()){

            return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация не запущена"));
        }
        PageRecursiveAction.setStopIndexing(true);

        return ResponseEntity.ok(new IndexingResponse(true, null));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexingPage(@RequestParam String url){
        indexingService.startIndexingOne(url);
        return ResponseEntity.ok(new IndexingResponse(true, null));//TODO исправить Response согласно тз

        //TODO создать Response в случае ошибки согласно тз
    }

    @GetMapping("/search")
    public ResponseEntity<IndexingResponse> searchText(@RequestParam(required = false) String query,
                                                       @RequestParam(required = false) String site,
                                                       @RequestParam(required = false) Integer offset,
                                                       @RequestParam(required = false) Integer limit){

        //TODO написать метод согласно тз

        return ResponseEntity.ok(new IndexingResponse(true, null));
    }

}
