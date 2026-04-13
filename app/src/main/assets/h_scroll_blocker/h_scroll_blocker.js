(function() {
    var style = document.createElement('style');
    style.innerHTML = `
        html, body {
            overflow-x: hidden !important;
            position: relative;
            width: 100%;
        }
    `;
    document.head.appendChild(style);
})();