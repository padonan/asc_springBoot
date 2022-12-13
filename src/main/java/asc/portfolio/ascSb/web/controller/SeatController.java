package asc.portfolio.ascSb.web.controller;

import asc.portfolio.ascSb.domain.user.User;
import asc.portfolio.ascSb.domain.user.UserRoleType;
import asc.portfolio.ascSb.jwt.LoginUser;
import asc.portfolio.ascSb.web.dto.seat.SeatSelectResponseDto;
import asc.portfolio.ascSb.service.seat.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/seat")
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{cafeName}")
    public ResponseEntity<List<SeatSelectResponseDto>> seatState(@PathVariable String cafeName) {
        if(cafeName.isEmpty()) {
            log.info("cafeName이 비어 있습니다.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(seatService.showCurrentSeatState(cafeName), HttpStatus.OK);
    }

    @PostMapping("/reservation/{seatNumber}")
    public ResponseEntity<String> reserveSeat(@LoginUser User user, @PathVariable int seatNumber) {

        //선택 된 카페가 없음.
        if (user.getCafe() == null) {
            return new ResponseEntity<>("Select a cafe first", HttpStatus.BAD_REQUEST);
        }

        Boolean isSuccess = seatService.reserveSeat(user, user.getCafe(), seatNumber);
        if (!isSuccess) {
            return new ResponseEntity<>("Failed", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @PostMapping("/exit")
    public ResponseEntity<String> exitSeat(@LoginUser User user) {

        Boolean isSuccess = seatService.exitSeat(user);
        if (!isSuccess) {
            return new ResponseEntity<>("No seat where the user sat", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @PostMapping("/exit-admin/{seatNumber}")
    public ResponseEntity<String> exitSeat(@LoginUser User admin, @PathVariable int seatNumber) {
        if (admin.getRole() == UserRoleType.ADMIN) {
            if (admin.getCafe() != null) {
                log.info("Exit Seat By Admin = {}", admin.getLoginId());
                seatService.exitSeatBySeatNumber(admin.getCafe(), seatNumber);
                return new ResponseEntity<>("Success", HttpStatus.OK);
            } else {
                log.warn("Set up a cafe field first");
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

        } else {
            log.info("Unauthorized User");
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
    }
}
