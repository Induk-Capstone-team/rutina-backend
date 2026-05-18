(function () {
    // 우클릭 방지
    document.addEventListener("contextmenu", function (event) {
        event.preventDefault();
    });

    // 개발자 도구 / 소스 보기 단축키 방지
    document.addEventListener("keydown", function (event) {
        const key = event.key.toLowerCase();

        // F12
        if (event.key === "F12") {
            event.preventDefault();
            return false;
        }

        // Ctrl + Shift + I / J / C
        if (event.ctrlKey && event.shiftKey && ["i", "j", "c"].includes(key)) {
            event.preventDefault();
            return false;
        }

        // Mac: Command + Option + I / J / C
        if (event.metaKey && event.altKey && ["i", "j", "c"].includes(key)) {
            event.preventDefault();
            return false;
        }

        // Ctrl + U: 페이지 소스 보기
        if (event.ctrlKey && key === "u") {
            event.preventDefault();
            return false;
        }

        // Ctrl + S: 페이지 저장
        if (event.ctrlKey && key === "s") {
            event.preventDefault();
            return false;
        }
    });

    // 드래그 방지
    document.addEventListener("dragstart", function (event) {
        event.preventDefault();
    });
})();