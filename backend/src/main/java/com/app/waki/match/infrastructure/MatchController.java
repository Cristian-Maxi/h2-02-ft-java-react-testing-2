package com.app.waki.match.infrastructure;

import com.app.waki.match.application.MatchService;
import com.app.waki.match.domain.Match;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    @GetMapping("/ligasUpdate")
    public ResponseEntity<Void> updateLigas() throws IOException, InterruptedException {
        service.UpdateMatches();  // Solo actualiza y guarda en la base de datos
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/getMatches/{code}")
    public ResponseEntity<List<Match>> getLigasMatches(@PathVariable("code") String code) {
        List<Match> matches = service.getMatchesWithinFiveDays(code.toUpperCase());
        return new ResponseEntity<>(matches, HttpStatus.OK);
    }
}
