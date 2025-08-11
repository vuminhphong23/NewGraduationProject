//package GraduationProject.forumikaa.controller;
//
//import GraduationProject.forumikaa.entity.Topic;
//import GraduationProject.forumikaa.service.TopicService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/topics")
//@CrossOrigin(origins = "http://localhost:3000")
//public class TopicController {
//
//    @Autowired
//    private TopicService topicService;
//
//    @GetMapping
//    public ResponseEntity<List<Topic>> getAllTopics() {
//        try {
//            List<Topic> topics = topicService.getAllTopics();
//            return ResponseEntity.ok(topics);
//        } catch (Exception e) {
//            System.err.println("Error getting topics: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(500).body(null);
//        }
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Topic> getTopicById(@PathVariable Long id) {
//        try {
//            Topic topic = topicService.getTopicById(id);
//            if (topic != null) {
//                return ResponseEntity.ok(topic);
//            } else {
//                return ResponseEntity.notFound().build();
//            }
//        } catch (Exception e) {
//            System.err.println("Error getting topic by ID: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(500).body(null);
//        }
//    }
//
//    @PostMapping
//    public ResponseEntity<Topic> createTopic(@RequestBody Topic topic) {
//        try {
//            Topic createdTopic = topicService.saveTopic(topic);
//            return ResponseEntity.ok(createdTopic);
//        } catch (Exception e) {
//            System.err.println("Error creating topic: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(500).body(null);
//        }
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Topic> updateTopic(@PathVariable Long id, @RequestBody Topic topic) {
//        try {
//            topic.setId(id);
//            Topic updatedTopic = topicService.saveTopic(topic);
//            return ResponseEntity.ok(updatedTopic);
//        } catch (Exception e) {
//            System.err.println("Error updating topic: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(500).body(null);
//        }
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> deleteTopic(@PathVariable Long id) {
//        try {
//            topicService.deleteTopic(id);
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            System.err.println("Error deleting topic: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(500).body(null);
//        }
//    }
//}
