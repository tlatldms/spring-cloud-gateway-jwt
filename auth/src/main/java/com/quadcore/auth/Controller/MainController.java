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

import javax.transaction.Transactional;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


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


    @PostMapping(path="/auth/refresh")
    public Map<String, Object>  requestForNewAccessToken(@RequestBody Map<String, String> m) {
        String username = null;
        Map<String, Object> map = new HashMap<>();
        String expiredAccessToken = m.get("accessToken");
        String refreshToken = m.get("refreshToken");
        logger.info("get expired access token: " + expiredAccessToken);

        try {
            username = jwtGenerator.getUsernameFromToken(expiredAccessToken);
        } catch (ExpiredJwtException e) {
            username = e.getClaims().getSubject();
            logger.info("username from expired access token: " + username);
        }
        if (username == null) throw new IllegalArgumentException();

        ValueOperations<String, Object> vop = redisTemplate.opsForValue();
        Token result = (Token) vop.get(username);
        String refreshTokenFromDb = result.getRefreshToken();
        logger.info("rtfrom db: " + refreshTokenFromDb);

        //user refresh token doesnt match with cache
        if (!refreshToken.equals(refreshTokenFromDb)) {
            map.put("errorCode", 58);
            return map;
        }

        //refresh token is expired
        if (jwtGenerator.isTokenExpired(refreshToken)) {
            map.put("errorCode", 57);
        }

        //generate access token if valid refresh token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken =  jwtGenerator.generateAccessToken(userDetails);
        map.put("errorCode", 10);
        map.put("accessToken", newAccessToken);
        return map;
    }

    @GetMapping(path="/user/normal")
    public Map<String, Object> onlyNormal() {
        Map<String, Object> map = new HashMap<>();
        map.put("errorCode", 10);
        return map;
    }

    @Transactional
    @PostMapping(path="/admin/deleteuser")
    public Map<String, Object> deleteUser (@RequestBody Map<String, String> m) {
        Map<String, Object> map = new HashMap<>();
        logger.info("delete user: " + m.get("username"));
        Long result = accountRepository.deleteByUsername(m.get("username"));
        logger.info("delete result: " + result);
        map.put("errorCode", 10);
        return map;
    }

    @GetMapping(path="/admin/getusers")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> map = new HashMap<>();
        map.put("errorCode", 10);
        map.put("users",  accountRepository.findAll());
        logger.info("users: " + map);
        return map;
    }

    @PostMapping(path="/auth/out")
    public Map<String, Object> logout(@RequestBody Map<String, String> m) {
        Map<String, Object> map = new HashMap<>();
        String accessToken = m.get("accessToken");
        String username = null;
        try {
            username = jwtGenerator.getUsernameFromToken(accessToken);
        } catch (ExpiredJwtException e) {
            username = e.getClaims().getSubject();
            logger.info("in logout: username: " + username);
        }

        redisTemplate.delete(username);
        //cache logout token for 10 minutes!
        logger.info(" logout ing : " + accessToken);
        redisTemplate.opsForValue().set(accessToken, true);
        redisTemplate.expire(accessToken, 10*6*1000, TimeUnit.MILLISECONDS);
        map.put("errorCode", 10);
        return map;
    }


    @PostMapping(path="/auth/name")
    public Map<String, Object> checker(@RequestBody Map<String, String> m) {
        Map<String, Object> map = new HashMap<>();
        String username = null;
        String accessToken = m.get("accessToken");
        try {
            username = jwtGenerator.getUsernameFromToken(accessToken);
        } catch (ExpiredJwtException e) {
            username = e.getClaims().getSubject();
            logger.info("in logout: username: " + username);
        }

        map.put("errorCode", 10);
        map.put("username", username);
        return map;
    }



}