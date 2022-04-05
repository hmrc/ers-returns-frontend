// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}

const backLink = document.getElementById("back-link")
backLink.addEventListener('click', function(e){
    e.preventDefault();
    window.history.back();
});
