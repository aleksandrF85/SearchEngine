package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.WebSite;
import searchengine.repository.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    SiteRepository siteRepository;

    @Autowired
    IndexingService indexingService;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        List<WebSite> sitesList = new ArrayList<>();
        siteRepository.findAll().forEach(site -> sitesList.add(site));
        total.setSites(sitesList.size());
        total.setIndexing(!indexingService.isIndexingInProgress());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(int i = 0; i < sitesList.size(); i++) {
            WebSite site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = site.getPages().size();
            int lemmas = site.getLemmaList().size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            long millis = site.getStatusTime().atZone(ZoneId.systemDefault()) // timezone
                    .toInstant() // Instant object
                    .toEpochMilli(); // milliseconds
            item.setStatusTime(millis);

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
