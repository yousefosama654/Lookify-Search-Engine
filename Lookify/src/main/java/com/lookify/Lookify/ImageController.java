package com.lookify.Lookify;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.json.simple.JSONObject;

@RestController
@RequestMapping("/path")
public class ImageController {
    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/{imagePath}")
    public JSONObject getWord(@PathVariable String imagePath) {
        String type = ImageRecognition.StartImageRecognition("C:\\Users\\sggln\\OneDrive\\Desktop\\" + imagePath);
        JSONObject answer = new JSONObject();
        answer.put("text", type);
        return answer;
    }
}
