(function () {
    const result = {
        themeColor: "",
        manifest: "",
        icons: []
    };

    function obtainTouchIcons() {
        var iconElement = document.querySelectorAll("link[rel='apple-touch-icon'],link[rel='apple-touch-icon-precomposed");
        var length = iconElement.length;
        for (var i = 0; i < length; i++) {
            var icon = {
                size: '',
                rel: '',
                href: ''
            };
            var item = iconElement[i];
            if (item.hasAttribute('sizes')) icon.size = item.sizes[0];
            icon.rel = item.rel;
            icon.href = item.href;
            result.icons.push(icon);
        }
    }

    function obtainThemeColor() {
        var meta = document.querySelector('meta[name=\"theme-color\"]');
        if (meta) result.themeColor = meta.content;
    }

    function obtainManifest() {
        var element = document.querySelector("link[rel='manifest']");
        if (element) {
            result.manifest = element.href;
            return true;
        }
        return false;
    }

    if (!obtainManifest()) {
        obtainThemeColor();
        obtainTouchIcons();
    }

    //        var info = JSON.stringify(result);
    //        window.app_native.onReceivedTouchIcons(document.URL, JSON.stringify(result));
    //        console.log(result);
    return JSON.stringify(result);
})();
