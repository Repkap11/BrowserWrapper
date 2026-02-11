(function() {
    let parent_ele = null;
    let observer = null;

    function attachObserver(parent) {
        observer = new MutationObserver(() => {
            const child_ele = parent.querySelector("div:nth-child(2)");
            if (child_ele) {
                child_ele.click();
            }
        });
        observer.observe(parent, { childList: true });
    }

    function findAndObserveParent() {
        const p = document.querySelector(".ytwTimelyActionsOverlayViewModelHost");

        if (p && p !== parent_ele) {
            parent_ele = p;
            if (observer) observer.disconnect();
            attachObserver(parent_ele);
        }
    }

    findAndObserveParent();
    setInterval(findAndObserveParent, 2000);
})();
