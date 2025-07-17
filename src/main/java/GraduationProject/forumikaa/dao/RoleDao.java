package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleDao extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}
//Tích hợp sẵn các phương thức CRUD:
//JpaRepository cung cấp các phương thức cơ bản để thao tác với dữ liệu như:
//
//save(): Lưu hoặc cập nhật dữ liệu.
//
//findAll(): Lấy tất cả các bản ghi.
//
//findById(): Tìm bản ghi theo ID.
//
//deleteById(): Xóa bản ghi theo ID.


//JpaRepository tự động cung cấp các phương thức để thực thi truy vấn SQL mà không cần
// phải viết mã truy vấn thủ công. Bạn có thể sử dụng các phương thức như findById, findByName,...
// mà không phải lo lắng về việc xây dựng câu lệnh SQL.


//Hỗ trợ Pagination và Sorting