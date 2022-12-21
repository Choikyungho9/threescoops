package control;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.threescoops.dto.PageBean;
import com.threescoops.exception.AddException;
import com.threescoops.exception.FindException;
import com.threescoops.exception.ModifyException;
import com.threescoops.exception.RemoveException;
import com.threescoops.service.RepBoardService;
import com.threescoops.util.Attach;
import com.threescoops.vo.Customer;
import com.threescoops.vo.RepBoard;

//@Controller
@RestController //@Controller + 각 메서드마다 @ResponseBody 붙여준다.
@RequestMapping("board/*")
public class RepBoardRestController {
	@Autowired
	private RepBoardService service;

	@PostMapping(value="new", produces = "application/json;charset=UTF-8")
	//@ResponseBody
	public ResponseEntity<?> write(HttpSession session, 
			@RequestPart   List<MultipartFile> f, //multiple속성인 경우 List타입으로 매핑 
			@RequestPart   MultipartFile fImg,
			RepBoard rb) 						// @RequestPart 와 @RequestBody는 절대 같이 못쓴다.
					throws AddException {
		String loginedId = (String)session.getAttribute("loginedId");
		Customer c = new Customer();
		c.setId(loginedId);
		rb.setBoardC(c);
		int boardNo = service.writeBoard(rb);	
		
		try {
			//파일업로드(f, fImg)작업
			com.threescoops.util.Attach.upload(boardNo, f);
		}catch(AddException e) {
		}
		try{
			com.threescoops.util.Attach.upload(boardNo, fImg);
		}catch(AddException e) {
		}										
		return new ResponseEntity<>(HttpStatus.OK);		
	}

	//@GetMapping(value="boardlist", produces = "application/json;charset=UTF-8")
	@GetMapping(value="list/{currentPage}", produces = "application/json;charset=UTF-8")
	//@ResponseBody
	public ResponseEntity<?> list(@PathVariable int currentPage) throws FindException{
		PageBean<RepBoard> pb = service.getPageBean(currentPage);//service.findAll();
		return new ResponseEntity<>(pb, HttpStatus.OK);
	}

	//@GetMapping(value="boarddetail", produces = "application/json;charset=UTF-8")
	@GetMapping(value="{boardNo}", produces = "application/json;charset=UTF-8")
	//@ResponseBody
	public ResponseEntity<?> detail(@PathVariable int boardNo) throws FindException{
		RepBoard rb = service.findByBoardNo(boardNo);
		//글번호별 첨부파일이름들 응답 
		List<String>fileNames = new ArrayList<>();
		String saveDirectory = "c:\\files";
		File dir = new File(saveDirectory); 
		String[] allFileNames = dir.list();
		for(String fn: allFileNames) {
			if(fn.startsWith(boardNo + "_")) {
				fileNames.add(fn);
			}
		}
		Map<String, Object> map = new HashMap<>();
		map.put("rb", rb);
		map.put("fileNames", fileNames);			
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@PostMapping(value="{boardNo}", produces = "application/json;charset=UTF-8")
	//@ResponseBody
	public ResponseEntity<?> modify(
			@RequestPart   List<MultipartFile> f, //multiple속성인 경우 List타입으로 매핑 
			@RequestPart   MultipartFile fImg,
			@PathVariable int boardNo, 
            String boardTitle, 
            String boardContent, 
            HttpSession session){
		
		// 글 '제목'만 수정하려면 요청전달데이터(글내용)에 null또는 ""이 전달될 것
		// 글 '내용'만 수정하려면 요청전달데이터(글제목)에 null또는 ""이 전달될 것
		System.out.println(boardTitle +  ":" + boardContent);
		
		RepBoard rb = new RepBoard();
		rb.setBoardNo(boardNo);
		rb.setBoardTitle(boardTitle);
		rb.setBoardContent(boardContent);
		
		Customer c = new Customer();
//		c.setId((String)session.getAttribute("loginedId"));
		c.setId("id1");
		rb.setBoardC(c);		
		try {
			service.modify(rb);
			
			boolean flag = false; //첨부안된 경우
			for(MultipartFile mf: f) {
				String orignName = mf.getOriginalFilename();
				long fileLength = mf.getSize();
				if(fileLength > 0 && !"".equals(orignName)) {
					flag = true;
					break;
				}
			}
			
			String orignImgName = fImg.getOriginalFilename();
			long imgFileLength = fImg.getSize();
			if(imgFileLength > 0 && !"".equals(orignImgName)) {
				flag = true;
			}
			
			if(flag) { //첨부된 경우
				//기존 첨부파일들을 모두 삭제 (c:\\files경로에서 boardNo값으로 시작하는 파일들을 찾아 삭제 )
				String saveDirectory = "c:\\files";
				File dir = new File(saveDirectory);
				File[] files = dir.listFiles();
				for(File f1: files) {
					if(f1.getName().startsWith(boardNo+"_")) {
						f1.delete();
					}
				}
				
				//새로운 첨부파일 저장
				//파일업로드(f, fImg)작업
				//TODO
			}
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (ModifyException e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(),  HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@DeleteMapping(value="{boardNo}", produces = "application/json;charset=UTF-8")
	//@ResponseBody
	public ResponseEntity<?> remove(@PathVariable int boardNo) throws RemoveException{
		service.remove(boardNo);
		//기존 첨부파일들을 모두 삭제 (c:\\files경로에서 boardNo값으로 시작하는 파일들을 찾아 삭제 )
		File dir = new File(Attach.SAVE_DIRECTORY);
		File[] files = dir.listFiles();
		for(File f1: files) {
			if(f1.getName().startsWith(boardNo+"_")) {
				Attach.remove(f1.getName());
			}
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value="reply/{parentNo}", produces = "application/json;charset=UTF-8")
	//@ResponseBody
	public ResponseEntity<?> reply(HttpSession session, 
			@PathVariable int parentNo,
			@RequestBody RepBoard rb) throws AddException{
		Customer c = new Customer();
//		c.setId((String)session.getAttribute("loginedId"));
		c.setId("id1");
		rb.setBoardC(c);
		rb.setParentNo(parentNo);
		service.writeReply(rb);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("removeAttach")
	//@ResponseBody
	public ResponseEntity<?> removeAttach(String fileName) {
		//파일이름으로 첨부파일 삭제하기
		if(Attach.remove(fileName)) {
			return new ResponseEntity<>(HttpStatus.OK);
		}else {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
