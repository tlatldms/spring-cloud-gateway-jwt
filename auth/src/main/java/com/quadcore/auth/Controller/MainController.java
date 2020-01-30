package com.quadcore.auth.Controller;

import com.quadcore.auth.Domain.Account;
import com.quadcore.auth.Domain.Token;
import com.quadcore.auth.Repository.AccountRepository;
import com.quadcore.auth.jwt.JwtGenerator;
import com.quadcore.auth.service.JwtUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping
public class MainController {
    private Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private JwtGenerator jwtGenerator;
    @Autowired
    private AuthenticationManager am;

    @Autowired
    private PasswordEncoder bcryptEncoder;

    @GetMapping(path="/")
    public String test() {
        logger.info("requestrequest");
        return "TEST";
    }



    @PostMapping(path="/auth/register")
    public Map<String, Object> addNewUser (@RequestBody Account account) {
        String un = account.getUsername();
        Map<String, Object> map = new HashMap<>();
        System.out.println("회원가입요청 아이디: "+un + "비번: " + account.getPassword());
        account.setUsername(un);
        account.setEmail(account.getEmail());
        if (un.equals("admin")) {
            account.setRole("ROLE_ADMIN");
        } else {
            account.setRole("ROLE_USER");
        }
        account.setPassword(bcryptEncoder.encode(account.getPassword()));
        map.put("errorCode", 10);
        accountRepository.save(account);
        return map;
    }

    @PostMapping(path = "/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> m) throws Exception {
        Map<String, Object> map = new HashMap<>();
        final String username = m.get("username");
        logger.info("test input username: " + username);

        am.authenticate(new UsernamePasswordAuthenticationToken(username, m.get("password")));


        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String accessToken = jwtGenerator.generateAccessToken(userDetails);
        final String refreshToken = jwtGenerator.generateRefreshToken(username);

        Token retok = new Token();
        retok.setUsername(username);
        retok.setRefreshToken(refreshToken);

        //generate Token and save in redis
        ValueOperations<String, Object> vop = redisTemplate.opsForValue();
        vop.set(username, retok);

        logger.info("generated access token: " + accessToken);
        logger.info("generated refresh token: " + refreshToken);
        map.put("errorCode", 10);
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        return map;
    }


    @PostMapping(path="/auth/checkemail")
    public Map<String, Object>  checkEmail (@RequestBody Map<String, String> m) {
        Map<String, Object> map = new HashMap<>();
        System.out.println("이메일체크 요청 이메일: " + m.get("email"));
        if (accountRepository.findByEmail(m.get("email")) == null) {
            map.put("errorCode", 10);
        }
        else map.put("errorCode", 53);
        return map;
    }

/*
    @PostMapping(path="/auth/refresh")
    public Map<String, Object>  requestForNewAccessToken(@RequestBody Map<String, String> m) {
        String accessToken = null;
        String refreshToken = null;
        String refreshTokenFromDb = null;
        String username = null;
        Map<String, Object> map = new HashMap<>();
        try {
            accessToken = m.get("accessToken");
            refreshToken = m.get("refreshToken");
            logger.info("access token in rnat: " + accessToken);
            try {
                username = jwtGenerator.getUsernameFromToken(accessToken);
            } catch (IllegalArgumentException e) {

            } catch (ExpiredJwtException e) { //expire됐을 때
                username = e.getClaims().getSubject();
                logger.info("username from expired access token: " + username);
            }

            if (refreshToken != null) { //refresh를 같이 보냈으면.
                try {
                    ValueOperations<String, Object> vop = redisTemplate.opsForValue();
                    Token result = (Token) vop.get(username);
                    refreshTokenFromDb = result.getRefreshToken();
                    logger.info("rtfrom db: " + refreshTokenFromDb);
                } catch (IllegalArgumentException e) {
                    logger.warn("illegal argument!!");
                }
                //둘이 일치하고 만료도 안됐으면 재발급 해주기.
                if (refreshToken.equals(refreshTokenFromDb) && !jwtTokenUtil.isTokenExpired(refreshToken)) {
                    final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    String newtok =  jwtTokenUtil.generateAccessToken(userDetails);
                    map.put("success", true);
                    map.put("accessToken", newtok);
                } else {
                    map.put("success", false);
                    map.put("msg", "refresh token is expired.");
                }
            } else { //refresh token이 없으면
                map.put("success", false);
                map.put("msg", "your refresh token does not exist.");
            }

        } catch (Exception e) {
            throw e;
        }
        logger.info("m: " + m);

        return map;
    }
*/

    /*
    @Transactional
    @PostMapping(path="/admin/deleteuser")
    public void deleteUser (@RequestBody Map<String, String> m) {
        logger.info("delete user: " + m.get("username"));
        Long result = accountRepository.deleteByUsername(m.get("username"));
        logger.info("delete result: " + result);
    }

    @GetMapping(path="/admin/getusers")
    public Iterable<Account> getAllUsers() {
        return accountRepository.findAll();
    }

    @GetMapping(path="/user/normal")
    public ResponseEntity<?> onlyNormal() {
        return new ResponseEntity(HttpStatus.OK);
    }


    @PostMapping(path="/newuser/out")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> m) {
        String username = null;
        String accessToken = m.get("accessToken");
        try {
            username = jwtTokenUtil.getUsernameFromToken(accessToken);
        } catch (IllegalArgumentException e) {} catch (ExpiredJwtException e) { //expire됐을 때
            username = e.getClaims().getSubject();
            logger.info("username from expired access token: " + username);
        }

        try {
            if (redisTemplate.opsForValue().get(username) != null) {
                //delete refresh token
                redisTemplate.delete(username);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("user does not exist");
        }

        //cache logout token for 10 minutes!
        logger.info(" logout ing : " + accessToken);
        redisTemplate.opsForValue().set(accessToken, true);
        redisTemplate.expire(accessToken, 10*6*1000, TimeUnit.MILLISECONDS);

        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping(path="/authcheck")
    public Map<String, Object> checker(@RequestBody Map<String, String> m) {
        String username = null;
        Map<String, Object> map = new HashMap<>();
        try {
            username = jwtTokenUtil.getUsernameFromToken(m.get("accessToken"));
        } catch (IllegalArgumentException e) {
            logger.warn("Unable to get JWT Token");
        }
        catch (ExpiredJwtException e) {
        }

        if (username != null) {
            map.put("success", true);
            map.put("username", username);
        } else {
            map.put("success", false);
        }
        return map;
    }
    @PostMapping(path = "/newuser/login")
    public Map<String, Object> login(@RequestBody Map<String, String> m) throws Exception {
        final String username = m.get("username");
        logger.info("test input username: " + username);
        try {
            am.authenticate(new UsernamePasswordAuthenticationToken(username, m.get("password")));
        } catch (Exception e){
            throw e;
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(username);

        Token retok = new Token();
        retok.setUsername(username);
        retok.setRefreshToken(refreshToken);

        //generate Token and save in redis
        ValueOperations<String, Object> vop = redisTemplate.opsForValue();
        vop.set(username, retok);

        logger.info("generated access token: " + accessToken);
        logger.info("generated refresh token: " + refreshToken);
        Map<String, Object> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        return map;
    }


     */


}