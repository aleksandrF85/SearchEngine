package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.responses.ErrorResponse;
import searchengine.dto.responses.Response;
import searchengine.dto.search.SearchResponse;
import searchengine.services.IndexingService;
import searchengine.services.PageRecursiveAction;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    @Autowired
    IndexingService indexingService;
    @Autowired
    SearchService searchService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<Object> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing(){
        if (indexingService.isIndexingInProgress()){

            return ResponseEntity.badRequest().body(new ErrorResponse(false, "Индексация уже запущена"));
        }
        PageRecursiveAction.setStopIndexing(false);
        new Thread(() -> indexingService.startIndexingAll()).start();

        return ResponseEntity.ok(new Response(true));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing(){
        if (!indexingService.isIndexingInProgress()){

            return ResponseEntity.badRequest().body(new ErrorResponse(false, "Индексация не запущена"));
        }
        PageRecursiveAction.setStopIndexing(true);

        return ResponseEntity.ok(new Response(true));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexingPage(@RequestParam String url){
        try {
            PageRecursiveAction.setStopIndexing(false);
            indexingService.startIndexingOne(url);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }

        return ResponseEntity.ok(new Response(true));
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchText(@RequestParam(defaultValue = "") String query,
                                                     @RequestParam(defaultValue = "") String site,
                                                     @RequestParam(defaultValue = "0") Integer offset,
                                                     @RequestParam(defaultValue = "20") Integer limit){

        SearchResponse response;
        try {
            response = searchService.startSearch(query, site, offset, limit);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }

        return ResponseEntity.ok(response);
    }
}
