function searchCharacters() {
	// 검색 버튼에 이벤트 연결
	document.getElementById('searchForm').addEventListener('submit', function(event) {
		event.preventDefault(); // 폼 제출 기본 동작 방지

		// 캐릭터 서버 및 캐릭터 이름 가져오기
		let server = document.getElementById('server').value;
		let characterName = document.getElementById('character').value;

		// URL 인코딩된 쿼리 문자열 생성 (서버와 캐릭터 이름 전송)
		let queryString = new URLSearchParams({
			server: server,
			characterName: characterName,
			page: 1 // 첫 번째 페이지로 시작
		}).toString();

		// 서버로 fetch 요청 (POST 요청)
		fetch(`/dnf/api/rest/search`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
				'Accept': 'application/json'
			},
			body: queryString // body에 쿼리 문자열 전달
		})
		.then(response => {
			if (!response.ok) {
				throw new Error('Network response was not ok');
			}
			return response.json(); // JSON 응답으로 변환
		})
		.then(data => {
			console.log(data)
			// 서버에서 필터링 및 페이징된 캐릭터 데이터 가져옴
			let characterArrays = data.characters;  // 서버에서 받은 캐릭터 목록
			let totalPages = data.totalPages;  // 총 페이지 수
			let currentPage = data.currentPage;  // 현재 페이지

			// 첫 번째 페이지 렌더링 및 페이징 버튼 생성
			renderPage(characterArrays);
			createPaginationControls(totalPages, currentPage);
		})
		.catch(error => {
			console.error('Error:', error);
		});
	});
}

function getCharterInfo(page) {
	// 캐릭터 서버 및 캐릭터 이름을 입력 필드에서 가져옴
	let server = document.getElementById('server').value;
	let characterName = document.getElementById('character').value;

	// URL 인코딩된 쿼리 문자열 생성 (페이지 번호 포함)
	let queryString = new URLSearchParams({
		server: server,
		characterName: characterName,
		page: page // 클릭된 페이지 번호로 요청
	}).toString();

	// 서버로 fetch 요청 (POST 요청)
	fetch(`/dnf/api/rest/search`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'Accept': 'application/json'
		},
		body: queryString // body에 쿼리 문자열 전달
	})
	.then(response => {
		if (!response.ok) {
			throw new Error('Network response was not ok');
		}
		return response.json(); // JSON 응답으로 변환
	})
	.then(data => {
		//console.log("Received Data:", data);  // 서버에서 받은 데이터를 콘솔에 출력하여 확인
		// 서버에서 필터링 및 페이징된 캐릭터 데이터 가져옴
		let characterArrays = data.characters;  // 서버에서 받은 캐릭터 목록
		let totalPages = data.totalPages;  // 총 페이지 수
		let currentPage = data.currentPage;  // 현재 페이지

		// 해당 페이지 데이터 렌더링
		renderPage(characterArrays);
		createPaginationControls(totalPages, currentPage);
	})
	.catch(error => {
		console.error('Error:', error);
	});
}

// 페이징 버튼을 생성하는 함수
function createPaginationControls(totalPages, currentPage) {
	let paginationDiv = document.getElementById('pagination');
	paginationDiv.innerHTML = ""; // 기존 버튼 초기화

	// 페이지 버튼 생성
	for (let i = 1; i <= totalPages; i++) {
		let pageButton = document.createElement('button');
		pageButton.textContent = i;
		pageButton.classList.add('page-button');
		if (i === currentPage) {
			pageButton.classList.add('active');
		}
		pageButton.addEventListener('click', () => {
			// 페이지 이동 시 서버에 해당 페이지 요청
			//console.log("클릭")
			getCharterInfo(i); // 클릭한 페이지 번호로 데이터 요청
		});
		paginationDiv.appendChild(pageButton);
	}
}

// 페이지별로 데이터 목록을 표시하는 함수
function renderPage(characterArrays) {
	//console.log(characterArrays);
	// 캐릭터 목록을 HTML로 렌더링
	let resultSection = document.getElementById("resultSection");
	resultSection.innerHTML = ""; // 기존 내용을 초기화

	characterArrays.forEach(character => {
		let charServerName = invertServerIdIntoKorean(character);
		let characterHtml = `
			<div class="charImg">
				<div class="charJopAndServer">
					<span class="charServer">${charServerName}</span>
					<div class="jobAndLevel">
						<span class="level">Lv.${character.level}</span>
						<span class="charJop">${character.jobGrowName}</span>
					</div>
				</div>
				<img class="charImgs" src="https://img-api.neople.co.kr/df/servers/${character.serverId}/characters/${character.characterId}?zoom=1">
				<div class="charInfo">
					<div class="fameInfo">
						<img class="fameImg" src="/img/img_fame.png">
						<span class="fame">${character.fame}</span>
					</div>
					<h2 class="charName">${character.characterName}</h2>
					<span class="adventureName">${character.adventureName}</span>
					<div class="rank">1904위</div>
				</div>
			</div>
		`;
		resultSection.innerHTML += characterHtml;
	});
}

// 서버아이디를 한글 서버 이름으로 바꿔주는 함수
function invertServerIdIntoKorean(character) {
    let serverName;
    let serverNameLists = document.getElementById("server");
    
    // 서버 리스트에서 서버 ID에 해당하는 한글 이름을 찾음
    for (let serverNameList of serverNameLists) {
        if (serverNameList.value == character.serverId) {
            serverName = serverNameList.innerText;
        }
    }
    
    //console.log("서버 ID: " + character.serverId + ", 한글 서버 이름: " + serverName);
    return serverName;
}
// 서버 리스트 가져오는 함수
function getAllServerLists() {
    fetch('/dnf/api/rest/getAllServer')  // 서버 리스트를 가져오는 API 호출
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json(); // JSON으로 응답 변환
        })
        .then(data => {
            // 서버 목록을 HTML 옵션으로 변환
            let serverArrays = data.rows;  // 서버 리스트를 받음
            let serverLists = `<option value="" disabled selected>서버 선택</option>
                               <option value="all">전체</option>`;  // 기본 옵션 추가

            serverArrays.forEach(server => {
                // 서버 ID와 이름을 이용해 옵션 요소 추가
                serverLists += `<option value="${server.serverId}">${server.serverName}</option>`;
            });

            // 서버 선택 필드에 옵션 리스트 추가
            document.getElementById("server").innerHTML = serverLists;
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });
}


