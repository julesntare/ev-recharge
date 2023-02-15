let userInfo = {},
  lat = "",
  lng = "";
firebase.auth().onAuthStateChanged((user) => {
  if (user) {
    docRef = db.collection("users").doc(user.uid);
    docRef
      .get()
      .then((doc) => {
        userInfo = { id: user.uid, ...doc.data() };

        if (userInfo.level != 1) {
          document.querySelector("#user-nav").style.display = "none";
        }

        if (document.querySelector("#stations-counter")) {
          // get total stations
          db.collection("stations")
            .where("status", "==", 1)
            .get()
            .then((querySnapshot) => {
              let totalStations = querySnapshot.size;
              document.querySelector("#stations-counter").innerHTML =
                totalStations;
            });

          // get total users
          db.collection("users")
            .where("level", "!=", 1)
            .get()
            .then((querySnapshot) => {
              let totalUsers = querySnapshot.size;
              document.querySelector("#users-counter").innerHTML = totalUsers;
            });

          // get total visits
          if (userInfo.level == 1) {
            db.collection("visited_stations")
              .get()
              .then((querySnapshot) => {
                let totalVisits = querySnapshot.size;
                document.querySelector("#visits-counter").innerHTML =
                  totalVisits;
              });

            // get total visits payments
            let totalPaid = 0;
            db.collection("visited_stations")
              .get()
              .then((querySnapshot) => {
                querySnapshot.forEach((doc) => {
                  totalPaid += doc.data().total_paid;
                });
                document.querySelector(
                  "#paid-counter"
                ).innerHTML = `${totalPaid} RWF`;
              });
          } else {
            // get stations for a certain user
            let stations = [];
            db.collection("stations")
              .where("user_id", "==", userInfo.id)
              .get()
              .then((querySnapshot) => {
                querySnapshot.forEach((doc) => {
                  // push id to array
                  stations.push(doc.id);
                  console.log(stations);
                  // get visited_stations station id document referencing
                  db.collection("visited_stations")
                    .where(
                      "station_id",
                      "==",
                      db.collection("stations").doc(stations[0])
                    )
                    .get()
                    .then((querySnapshot) => {
                      let totalVisits = querySnapshot.size;
                      document.querySelector("#visits-counter").innerHTML =
                        totalVisits;
                    });

                  // get total visits payments
                  let totalPaid = 0;
                  db.collection("visited_stations")
                    // set value as station document reference in where clause
                    .where(
                      "station_id",
                      "==",
                      db.collection("stations").doc(stations[0])
                    )
                    .get()
                    .then((querySnapshot) => {
                      querySnapshot.forEach((doc) => {
                        totalPaid += doc.data().total_paid;
                        console.log(totalPaid);
                      });
                      document.querySelector(
                        "#paid-counter"
                      ).innerHTML = `${totalPaid} RWF`;
                    });
                });
              });
          }
        }

        // load all tables
        // display stations data
        if (document.querySelector("#stationsDataTable")) {
          let dataSet = [];
          if (userInfo.level != 1) {
            document.querySelector("#add-new-station").style.display = "none";
          }

          db.collection("stations")
            .get()
            .then(function (querySnapshot) {
              querySnapshot.forEach(function (doc) {
                dataSet.push([
                  doc.data().name,
                  `${doc.data().address} (Coords: ${
                    doc.data().location._lat
                  }, ${doc.data().location._long})`,
                  doc.data().image_path
                    ? `<img src="${
                        doc.data().image_path
                      }" alt="station image" width="100px" height="100px" />`
                    : "N/A",
                  userInfo.level != 1
                    ? "N/A"
                    : `<button data-toggle="modal"
          data-target="#stationModal" class="btn btn-sm btn-primary" onclick="modifyStationDetails('${doc.id}')"><i class="fa fa-edit"></i></button> <button class="btn btn-sm btn-danger" onclick="deleteStation('${doc.id}')"><i class="fa fa-trash"></i></button>`,
                ]);
              });

              $("#stationsDataTable").DataTable({
                data: dataSet,
                columns: [
                  { title: "Name" },
                  { title: "Address" },
                  { title: "Image" },
                  { title: "Actions" },
                ],
              });
            });
        }

        // display users data
        if (document.querySelector("#usersDataTable")) {
          let dataSet = [];
          db.collection("users")
            .where("level", "!=", 1)
            .get()
            .then(function (querySnapshot) {
              querySnapshot.forEach(function (doc) {
                dataSet.push([
                  doc.data().firstname + " " + doc.data().lastname,
                  doc.data().email,
                  doc.data().phone ? doc.data().phone : "N/A",
                  doc.data().img
                    ? `<a href="${
                        doc.data().img
                      }" target="_blank">Open in New Tab 
          <i class="fas fa-external-link-square-alt"></i></a>`
                    : "N/A",
                  doc.data().level == 2 ? "Station" : "Vehicle",
                ]);
              });

              $("#usersDataTable").DataTable({
                data: dataSet,
                columns: [
                  { title: "Full Name" },
                  { title: "Email" },
                  { title: "Mobile No." },
                  { title: "Image" },
                  { title: "User Type" },
                ],
              });
            });
        }

        // display visits data
        if (document.querySelector(".visitsDataTable")) {
          let dataSet = [],
            station_info = [],
            user_info = [];
          db.collection("visited_stations")
            .get()
            .then(function (querySnapshot) {
              querySnapshot.forEach(function (doc) {
                doc
                  .data()
                  .station_id.get()
                  .then((station) => {
                    station_info.push(station.data());

                    doc
                      .data()
                      .user_id.get()
                      .then((user) => {
                        console.log(user.data());
                        user_info.push(user.data());
                        dataSet.push([
                          "<i class='fa fa-caret-down'></i> " +
                            station_info[0].name,
                          user_info[0].firstname + " " + user_info[0].lastname,
                          doc.data().payment_status,
                          doc.data().total_paid,
                          doc.data().payment_method,
                          doc.data().account_no,
                          doc.data().visited_on.toDate().toLocaleString(),
                        ]);

                        let visitsTable = $(".visitsDataTable").DataTable({
                          data: dataSet,
                          columns: [
                            { title: "Station Info" },
                            { title: "Vehicle Info" },
                            { title: "Payment Status" },
                            { title: "Total Paid Amount" },
                            { title: "Payment Method" },
                            { title: "Payment Account" },
                            { title: "Visited On" },
                          ],
                          bDestroy: true,
                        });

                        $(".visitsDataTable tbody").on(
                          "click",
                          "td.sorting_1",
                          function () {
                            var tr = $(this).closest("tr");
                            var row = visitsTable.row(tr);

                            if (row.child.isShown()) {
                              // This row is already open - close it
                              row.child.hide();
                              tr.removeClass("shown");
                            } else {
                              // Open this row
                              row
                                .child(format(station_info[0], user_info[0]))
                                .show();
                              tr.addClass("shown");
                            }
                          }
                        );
                      });
                  });
              });
            });
        }

        if (document.querySelector("#user-card")) {
          if (userInfo.level != 1) {
            document.querySelector("#user-card").style.display = "none";
          } else {
            document.querySelector("#user-card").style.display = "block";
          }
        }
        if (document.querySelector("#profileName")) {
          document.querySelector("#profileName").innerHTML =
            doc.data().firstname + " " + doc.data().lastname;
          document.querySelector("#profileImage").src = doc.data().img
            ? doc.data().img
            : "img/default_avatar.png";
        }
      })
      .catch((error) => {
        console.log("Error getting document:", error);
      });
  } else {
    document.querySelector("#login-box").style.display = "flex";
    document.querySelector(".avatar").style.display = "none";
  }
});

document.querySelector("#logout").addEventListener("click", (e) => {
  e.preventDefault();
  firebase.auth().signOut();
  window.location = "login.html";
});

if ("geolocation" in navigator) {
  navigator.geolocation.getCurrentPosition(
    (position) => {
      lat = position.coords.latitude;
      lng = position.coords.longitude;
      return;
    },
    (error) => {
      console.log(error);
    }
  );
} else {
  alert("Please try to enable your location.");
  console.log("geolocation is not enabled");
}

const getCoordinates = (val) => {
  document.querySelector("#stationLatitude").value = lat;
  document.querySelector("#stationLongitude").value = lng;
};

// add station to firestore
const addStation = () => {
  document.querySelector("#stationName").value = "";
  document.querySelector("#stationAddress").value = "";
  document.querySelector("#stationLatitude").value = "";
  document.querySelector("#stationLongitude").value = "";
  document.querySelector("#stationCallCenter").value = "";
  document.querySelector("#stationForm").style.display = "block";
  document.querySelector("#stationEditForm").style.display = "none";
  document.querySelector("#stationForm").addEventListener("click", (e) => {
    e.preventDefault();
    let name = document.querySelector("#stationName").value;
    let address = document.querySelector("#stationAddress").value;
    let stationCallCenter = document.querySelector("#stationCallCenter").value;
    let stationLatitude = document.querySelector("#stationLatitude").value;
    let stationLongitude = document.querySelector("#stationLongitude").value;
    let profileImage = document.querySelector("#stationImage").files[0];
    let storageRef = storage.ref();

    if (profileImage != undefined) {
      let thisref = storageRef
        .child("stations_images")
        .child(profileImage.name)
        .put(profileImage);
      thisref.on("state_changed", function (snapshot) {
        snapshot.ref.getDownloadURL().then((url) => {
          db.collection("stations")
            .add({
              name: name,
              address: address,
              mobile_no: stationCallCenter,
              location: new firebase.firestore.GeoPoint(
                stationLatitude,
                stationLongitude
              ),
              image_path: url,
              added_on: new Date(),
              status: 1,
              user_id: userInfo.id,
            })
            .then((docRef) => {
              window.location = "stations.html";
            })
            .catch((error) => {
              console.error("Error adding document: ", error);
            });
        });
      });
    } else {
      db.collection("stations")
        .add({
          name: name,
          address: address,
          location: new firebase.firestore.GeoPoint(
            stationLatitude,
            stationLongitude
          ),
          image_path: "",
          added_on: new Date(),
          status: 1,
        })
        .then((_docRef) => {
          window.location = "stations.html";
        })
        .catch((error) => {
          console.error("Error adding document: ", error);
        });
    }
  });
};

// delete station
const deleteStation = (id) => {
  db.collection("stations")
    .doc(id)
    .delete()
    .then(() => {
      window.location = "stations.html";
    })
    .catch((error) => {
      console.error("Error removing document: ", error);
    });
};

// modify station
const modifyStationDetails = (id) => {
  db.collection("stations")
    .doc(id)
    .get()
    .then((doc) => {
      document.querySelector("#stationName").value = doc.data().name;
      document.querySelector("#stationAddress").value = doc.data().address;
      document.querySelector("#stationLatitude").value =
        doc.data().location.latitude;
      document.querySelector("#stationLongitude").value =
        doc.data().location.longitude;
      document.querySelector("#stationForm").style.display = "none";
      document.querySelector("#stationEditForm").style.display = "block";
      document
        .querySelector("#stationEditForm")
        .addEventListener("click", (e) => {
          e.preventDefault();
          let name = document.querySelector("#stationName").value;
          let address = document.querySelector("#stationAddress").value;
          let stationLatitude =
            document.querySelector("#stationLatitude").value;
          let stationLongitude =
            document.querySelector("#stationLongitude").value;
          let profileImage = document.querySelector("#stationImage").files[0];
          let storageRef = storage.ref();

          if (profileImage != undefined) {
            let thisref = storageRef
              .child("stations_images")
              .child(profileImage.name)
              .put(profileImage);
            thisref.on("state_changed", function (snapshot) {
              snapshot.ref.getDownloadURL().then((url) => {
                db.collection("stations")
                  .doc(id)
                  .update({
                    name: name,
                    address: address,
                    location: new firebase.firestore.GeoPoint(
                      stationLatitude,
                      stationLongitude
                    ),
                    image_path: url,
                    added_on: new Date(),
                  })
                  .then((_docRef) => {
                    window.location = "stations.html";
                  })
                  .catch((error) => {
                    console.error("Error adding document: ", error);
                  });
              });
            });
          } else {
            db.collection("stations")
              .doc(id)
              .update({
                name: name,
                address: address,
                location: new firebase.firestore.GeoPoint(
                  stationLatitude,
                  stationLongitude
                ),
                added_on: new Date(),
              })
              .then((_docRef) => {
                window.location = "stations.html";
              })
              .catch((error) => {
                console.error("Error adding document: ", error);
              });
          }
        });
    })
    .catch((error) => {
      console.log("Error getting document:", error);
    });
};

function format(station_data, user_data) {
  return `<tr>
    <td>
      <div class="row">
        <div class="col-md-12">
          <h5>Station Info</h5><hr/>
          <p>Name: ${station_data.name}</p>
          <p>Address: ${station_data.address}</p>
          <p>Coordinates: Lat: ${station_data.location._lat}, Long: ${
    station_data.location._long
  }</p>
        </div>
        </div>
    </td>
    <td>
      <div class="row">
        <div class="col-md-12">
          <h5>Vehicle Info</h5><hr/>
          <p>Name: ${user_data.firstname} ${user_data.lastname}</p>
          <p>Email: ${user_data.email}</p>
          <p>Mobile No.: ${user_data.phone ? user_data.phone : "N/A"}</p>
        </div>
        </div>
    </td>
    </tr>`;
}

// assign stations to users
if (document.querySelector("#assign_form")) {
  // get select ids
  let stationsContainer = document.querySelector("#stationsSelect");
  let usersContainer = document.querySelector("#usersSelect");

  // load all stations from firebase
  db.collection("stations")
    .get()
    .then((querySnapshot) => {
      querySnapshot.forEach((doc) => {
        let option = document.createElement("option");
        option.value = doc.id;
        option.innerHTML = doc.data().name;
        stationsContainer.appendChild(option);
      });
    });

  // load all users from firebase
  db.collection("users")
    .where("level", "==", 2)
    .get()
    .then((querySnapshot) => {
      querySnapshot.forEach((doc) => {
        let option = document.createElement("option");
        option.value = doc.id;
        option.innerHTML = doc.data().firstname + " " + doc.data().lastname;
        usersContainer.appendChild(option);
      });
    });
}

document.querySelector("#assignStation").addEventListener("click", (e) => {
  e.preventDefault();

  // get station/user selected option
  let stationSelected = document.querySelector("#stationsSelect").value;
  let userSelected = document.querySelector("#usersSelect").value;

  // update station with userSelected in firebase
  db.collection("stations")
    .doc(stationSelected)
    .update({
      user_id: userSelected,
    })
    .then(() => {
      window.location = "stations.html";
    })
    .catch((error) => {
      console.error("Error adding document: ", error);
    });
});

const assignStations = () => {
  let user_id = document.querySelector("#user_id").value;
  let station_id = document.querySelector("#station_id").value;
  db.collection("users")
    .doc(user_id)
    .update({
      station_id: station_id,
    })
    .then(() => {
      window.location = "stations.html";
    })
    .catch((error) => {
      console.error("Error adding document: ", error);
    });
};
