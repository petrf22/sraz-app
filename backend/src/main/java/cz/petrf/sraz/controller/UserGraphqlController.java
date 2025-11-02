package cz.petrf.sraz.controller;

import org.springframework.stereotype.Controller;

@Controller // Spring GraphQL
public class UserGraphqlController {

//    @QueryMapping   // @QueryMapping("userProfile")  -> query userProfile
//    @PreAuthorize("hasRole('USER')")
//    public UserProfile userProfile(Authentication auth) {
//        return UserProfile.builder()
//                          .username(auth.getName())
//                          .roles(auth.getAuthorities()
//                                     .stream()
//                                     .map(GrantedAuthority::getAuthority)
//                                     .toList())
//                          .build();
//    }
//
//    @MutationMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public User createUser(@Argument String username, @Argument String password) {
//        return userService.create(username, password);
//    }
}