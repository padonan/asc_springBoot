package asc.portfolio.ascSb.domain.seat;
import asc.portfolio.ascSb.domain.cafe.Cafe;
import asc.portfolio.ascSb.domain.seatreservationinfo.QSeatReservationInfo;
import asc.portfolio.ascSb.domain.seatreservationinfo.SeatReservationInfo;
import asc.portfolio.ascSb.domain.seatreservationinfo.SeatReservationInfoRepository;
import asc.portfolio.ascSb.domain.seatreservationinfo.SeatReservationInfoStateType;
import asc.portfolio.ascSb.domain.ticket.QTicket;
import asc.portfolio.ascSb.domain.ticket.Ticket;
import asc.portfolio.ascSb.web.dto.seat.SeatSelectResponseDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static asc.portfolio.ascSb.domain.cafe.QCafe.*;
import static asc.portfolio.ascSb.domain.seat.QSeat.*;
import static asc.portfolio.ascSb.domain.seatreservationinfo.QSeatReservationInfo.seatReservationInfo;
import static asc.portfolio.ascSb.domain.ticket.QTicket.*;

@Slf4j
@RequiredArgsConstructor
@Repository
public class SeatCustomRepositoryImpl implements SeatCustomRepository {

    private final JPAQueryFactory query;

    private final SeatReservationInfoRepository seatReservationInfoRepository;

    @Override
    public void updateAllReservedSeatStateWithFixedTermTicket() {

        // Fixed-Term Ticket Update
        List<Seat> seatList = query
                .selectFrom(seat)
                .join(seat.ticket, ticket)
                .where(seat.seatState.eq(SeatStateType.RESERVED),
                        ticket.productLabel.contains("FIXED-TERM"),
                        ticket.fixedTermTicket.after(LocalDateTime.now()))
                .fetch();

        // 만료된 Fixed-Term Ticket 에 따른 Seat, seatReservationInfo 종료 처리
        for (Seat seatOne : seatList) {
            exitSeatBySeatEntity(seatOne, null);
        }
    }

    @Override
    public void updateAllReservedSeatStateWithPartTimeTicket() {
        // Part-Time Ticket Update
        List<Seat> seatList = query
                .selectFrom(seat)
                .join(seat.ticket, ticket)
                .where(seat.seatState.eq(SeatStateType.RESERVED),
                        ticket.productLabel.contains("PART-TIME"))
                .fetch();

        for (Seat seatOne : seatList) {
            exitSeatBySeatEntity(seatOne, null);
        }
    }

    @Override
    public void updateAllReservedSeatStateWithStartTime() {
        List<SeatReservationInfo> seatRezInfoList = query
                .selectFrom(seatReservationInfo)
                .where(seatReservationInfo.isValid.eq(SeatReservationInfoStateType.VALID),
                        seatReservationInfo.endTime.after(LocalDateTime.now()))
                .fetch();

        for (SeatReservationInfo info : seatRezInfoList) {
            Seat findSeat = findByCafeNameAndSeatNumber(info.getCafeName(), info.getSeatNumber());
            exitSeatBySeatEntity(findSeat, info);
        }
    }

    @Override
    public List<Seat> getAlmostFinishedSeatListWithFixedTermTicket(Long minute) {
        //after(10분전) ~ before(9분전) ~~~ 현재
        return query
                .selectFrom(seat)
                .join(seat.ticket, ticket)
                .where(seat.seatState.eq(SeatStateType.RESERVED),
                        ticket.productLabel.contains("FIXED-TERM"),
                        ticket.fixedTermTicket.after(LocalDateTime.now().minusMinutes(minute)),
                        ticket.fixedTermTicket.before(LocalDateTime.now().minusMinutes(minute - 1)))
                .fetch();
    }

    @Override
    public List<Seat> getAlmostFinishedSeatListWithStartTime(Long minute) {
        //after(10분전) ~ before(9분전) ~~~ 현재
        List<SeatReservationInfo> seatRezInfoList = query
                .selectFrom(seatReservationInfo)
                .where(seatReservationInfo.isValid.eq(SeatReservationInfoStateType.VALID),
                        seatReservationInfo.endTime.after(LocalDateTime.now().minusMinutes(minute)),
                        seatReservationInfo.endTime.before(LocalDateTime.now().minusMinutes(minute - 1)))
                .fetch();

        List<Seat> seatList = new ArrayList<>();
        for (SeatReservationInfo info : seatRezInfoList) {
            Seat findSeat = findByCafeNameAndSeatNumber(info.getCafeName(), info.getSeatNumber());
            seatList.add(findSeat);
        }

        return seatList;
    }

    private void exitSeatBySeatEntity(Seat seatOne, SeatReservationInfo seatRezInfoEntity) {
        String cafeName = seatOne.getCafe().getCafeName();

        if (seatRezInfoEntity == null) {
            seatRezInfoEntity = seatReservationInfoRepository.findValidSeatRezInfoByCafeNameAndSeatNumber(cafeName, seatOne.getSeatNumber());
        }

        Ticket ticketOne = seatOne.getTicket();

        log.debug("Seat 사용 종료. cafeName={}, seatNumber={}", cafeName, seatOne.getSeatNumber());
        ticketOne.exitUsingTicket(seatRezInfoEntity.updateTimeInUse());
        seatOne.exitSeat();
        seatRezInfoEntity.endUsingSeat();
    }

//    public void updateSeatState(String cafeName) {
//        List<Seat> seatList = query
//                .selectFrom(seat)
//                .where(seat.cafe.cafeName.eq(cafeName))
//                .orderBy(seat.seatNumber.asc())
//                .fetch();
//
//        for (Seat seat : seatList) {
//            if (seat.getSeatState() == SeatStateType.RESERVED) {
//                if (!seat.getTicket().isValidTicket()) {
//                    log.info("Update SeatState SeatNumber={}", seat.getSeatNumber());
//                    seat.setSeatStateTypeUnReserved();
//                }
//            }
//        }
//    }

    public List<SeatSelectResponseDto> findSeatNumberAndSeatStateList(String cafeName) {
        //updateSeatState(cafeName);

        return query
                .select(Projections.bean(SeatSelectResponseDto.class, seat.seatNumber, seat.seatState))
                .from(seat)
                .where(seat.cafe.cafeName.eq(cafeName))
                .orderBy(seat.seatNumber.asc())
                .fetch();
    }

    @Override
    public Seat findByCafeAndSeatNumber(Cafe cafeObject, Integer seatNumber) {
        return query
                .select(seat)
                .from(seat)
                .join(seat.cafe, cafe)
                .where(cafe.eq(cafeObject), seat.seatNumber.eq(seatNumber))
                .fetchOne();
    }

    @Override
    public Seat findByCafeNameAndSeatNumber(String cafeName, int seatNumber) {
        return query
                .select(seat)
                .from(seat)
                .join(seat.cafe, cafe)
                .where(cafe.cafeName.eq(cafeName), seat.seatNumber.eq(seatNumber))
                .fetchOne();
    }
}
