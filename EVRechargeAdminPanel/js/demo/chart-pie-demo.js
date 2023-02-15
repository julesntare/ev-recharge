// Set new default font family and font color to mimic Bootstrap's default styling
(Chart.defaults.global.defaultFontFamily = "Nunito"),
  '-apple-system,system-ui,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif';
Chart.defaults.global.defaultFontColor = "#858796";

// count total payments in each category
let totalCash = 0;
let totalMobile = 0;
let totalBank = 0;

db.collection("visited_stations")
  .get()
  .then((querySnapshot) => {
    querySnapshot.forEach((doc) => {
      let payment_key = doc.data().payment_method;
      if (payment_key == "Cash") {
        totalCash += doc.data().total_paid;
      }
      if (payment_key == "MoMo") {
        totalMobile += doc.data().total_paid;
      }
      if (payment_key == "Bank") {
        totalBank += doc.data().total_paid;
      }
    });

    // Pie Chart Example
    var ctx = document.getElementById("myPieChart");
    new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: ["Cash", "Mobile Money", "Bank Acc"],
        datasets: [
          {
            data: [totalCash, totalMobile, totalBank],
            backgroundColor: ["#4e73df", "#1cc88a", "#36b9cc"],
            hoverBackgroundColor: ["#2e59d9", "#17a673", "#2c9faf"],
            hoverBorderColor: "rgba(234, 236, 244, 1)",
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        tooltips: {
          backgroundColor: "rgb(255,255,255)",
          bodyFontColor: "#858796",
          borderColor: "#dddfeb",
          borderWidth: 1,
          xPadding: 15,
          yPadding: 15,
          displayColors: false,
          caretPadding: 10,
        },
        legend: {
          display: false,
        },
        cutoutPercentage: 80,
      },
    });
  });
