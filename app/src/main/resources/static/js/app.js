$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                var formData = $(this).serializeArray();
                // Change format of interstitial
                formData = formData.map((e) => {
                    if(e.name=='interstitial')
                    {return {name: e.name, value: true}}
                    else return e
                })
                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: formData, // jQuery will handle the serialization
                    success: function (msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });