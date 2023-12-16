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
                        // If the user wants a QR code, display it as a button that redirects to /{id}/qr
                        $("#qr-result").html(
                            qr ? "<a href='" + msg.qr + "'>"
                            +"<div class='alert alert-success group rounded-full bg-gray-100 p-1.5 transition-all duration-75 hover:scale-105 hover:bg-blue-100 active:scale-95'>"
                            + "<svg xmlns='http://www.w3.org/2000/svg' width='4em' height='4em' preserveAspectRatio='xMidYMid meet' viewBox='0 0 32 32' className='text-gray-700 transition-all group-hover:text-blue-800'>"
                            + "<path fill='currentColor' d='M24 28v-2h2v2zm-6-4v-2h2v2zm0 6h4v-2h-2v-2h-2v4zm8-4v-4h2v4zm2 0h2v4h-4v-2h2v-2zm-2-6v-2h4v4h-2v-2h-2zm-2 0h-2v4h-2v2h4v-6zm-6 0v-2h4v2zM6 22h4v4H6z'></path>"
                            + "<path fill='currentColor' d='M14 30H2V18h12zM4 28h8v-8H4zM22 6h4v4h-4z'></path>"
                            + "<path fill='currentColor' d='M30 14H18V2h12zm-10-2h8V4h-8zM6 6h4v4H6z'></path>"
                            + "<path fill='currentColor' d='M14 14H2V2h12ZM4 12h8V4H4Z'></path>"
                            + "</svg>"
                            + "<div>Get QR Code</div>"
                            +"</div></a>" : "");
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });