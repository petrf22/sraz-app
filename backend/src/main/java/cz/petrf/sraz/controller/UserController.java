package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.Role;
import cz.petrf.sraz.db.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class UserController {
    private final RoleRepository roleRepo;

    @QueryMapping
    public List<Role> roles() {
        return roleRepo.findAll();
    }

//    @QueryMapping
//    public User users(@Argument String id) {
//        return Book.getById(id);
//    }
//
//    @SchemaMapping
//    public Role author(Book book) {
//        return Author.getById(book.authorId());
//    }
}