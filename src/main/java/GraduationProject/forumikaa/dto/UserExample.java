package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.User;
import lombok.Data;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

/**
 * UserExample class để sử dụng với Query By Example (QBE)
 * Cho phép tìm kiếm user dựa trên các thuộc tính được set
 */
@Data
public class UserExample {
    
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Boolean enabled;
    
    /**
     * Tạo Example<User> từ UserExample
     */
    public Example<User> toExample() {
        User user = new User();
        
        if (username != null) user.setUsername(username);
        if (email != null) user.setEmail(email);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        if (enabled != null) user.setEnabled(enabled);
        
        // Tạo ExampleMatcher với các quy tắc matching
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING) // LIKE %value%
                .withIgnoreCase() // Bỏ qua case
                .withIgnoreNullValues(); // Bỏ qua các giá trị null
        
        return Example.of(user, matcher);
    }
    
    /**
     * Tạo Example<User> với matching chính xác (exact match)
     */
    public Example<User> toExactExample() {
        User user = new User();
        
        if (username != null) user.setUsername(username);
        if (email != null) user.setEmail(email);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        if (enabled != null) user.setEnabled(enabled);
        
        // Tạo ExampleMatcher với matching chính xác
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT) // Exact match
                .withIgnoreCase() // Bỏ qua case
                .withIgnoreNullValues(); // Bỏ qua các giá trị null
        
        return Example.of(user, matcher);
    }
}







