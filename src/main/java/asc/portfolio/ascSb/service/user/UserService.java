package asc.portfolio.ascSb.service.user;

import asc.portfolio.ascSb.domain.user.User;
import asc.portfolio.ascSb.web.dto.user.UserQrAndNameResponseDto;
import asc.portfolio.ascSb.web.dto.user.UserSignupDto;

import java.util.List;

public interface UserService {

  Long signUp(UserSignupDto signUpDto);

  User checkPassword(String loginId, String password);

  User checkJsonWebToken(String jwt);

  List<UserQrAndNameResponseDto> userQrAndName(Long id);
}
