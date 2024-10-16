package com.app.waki.match.application;

import com.app.waki.match.domain.*;
import com.app.waki.match.domain.Match;
import com.app.waki.match.domain.MatchResponse;
import com.app.waki.match.domain.Odds;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final TeamHomeRepository teamHomeRepository;
    private final TeamAwayRepository teamAwayRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random(); // Generador de números aleatorios

    public MatchServiceImpl(MatchRepository matchRepository, TeamHomeRepository teamHomeRepository, TeamAwayRepository teamAwayRepository, ObjectMapper objectMapper) {
        this.matchRepository = matchRepository;
        this.teamHomeRepository = teamHomeRepository;
        this.teamAwayRepository = teamAwayRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void UpdateMatches() {
        List<String> codes = List.of("PL", "CL", "CLI", "ELC");
        try {
            List<Long> existingIds = matchRepository.findAllIds();
            Map<Long, Match> existingMatchesMap = matchRepository.findAllById(existingIds).stream()
                    .collect(Collectors.toMap(Match::getId, match -> match));
            for (String code : codes) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.football-data.org/v4/competitions/"+code+"/matches/?dateFrom=2024-10-01&dateTo=2024-10-30"))
                        .header("X-Auth-Token", "c83c236d84604a11b92b78aa10631d05")
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                MatchResponse matchResponse = objectMapper.readValue(response.body(), MatchResponse.class);

                for (Match match : matchResponse.getMatches()) {
                    if (match.getMatch_odds() == null) {
                        Odds odds = new Odds();
                        odds.setHome_team(new BigDecimal(1.0 + (3.0 - 1.0) * random.nextDouble()).setScale(2, RoundingMode.HALF_UP).doubleValue());
                        odds.setAway_team(new BigDecimal(1.0 + (3.0 - 1.0) * random.nextDouble()).setScale(2, RoundingMode.HALF_UP).doubleValue());
                        odds.setDraw(new BigDecimal(1.0 + (3.0 - 1.0) * random.nextDouble()).setScale(2, RoundingMode.HALF_UP).doubleValue());
                        match.setMatch_odds(odds);
                    }

                    // Al crear la DB por primera vez comentar esto, luego de creada descomentar y volver a crear
                    //-------------------------------------------------------------------------------------------------//
                    Optional<TeamHome> existingHomeTeam = teamHomeRepository.findByName(match.getHomeTeam().getName());
                    if (existingHomeTeam.isPresent()) {
                        match.setHomeTeam(existingHomeTeam.get());
                    } else {
                        match.setHomeTeam(teamHomeRepository.save(match.getHomeTeam())); // Guardar nuevo equipo
                    }

                    // Manejo del equipo visitante
                    Optional<TeamAway> existingAwayTeam = teamAwayRepository.findByName(match.getAwayTeam().getName());
                    if (existingAwayTeam.isPresent()) {
                        match.setAwayTeam(existingAwayTeam.get());
                    } else {
                        match.setAwayTeam(teamAwayRepository.save(match.getAwayTeam())); // Guardar nuevo equipo
                    }
                    //-------------------------------------------------------------------------------------------------//
                    Match existingMatch = existingMatchesMap.get(match.getId());
                    if (existingMatch != null) {
                        existingMatch.updateFrom(match);
                        matchRepository.save(existingMatch);
                    } else {
                        matchRepository.save(match);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error saving match: " + e.getMessage());
        }
    };

    @Override
    public List<Match> findAllMatches() {
        return matchRepository.findAll();
    }

    @Override
    public Optional<Match> findById(Long id) {
        return matchRepository.findById(id);
    }

    @Override
    public List<Match> getMatchesWithinFiveDays(String code) {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime fiveDaysLater = today.plusDays(5);
        return matchRepository.findMatchesByCompetitionAndDateRange(code, today, fiveDaysLater);
    }
}