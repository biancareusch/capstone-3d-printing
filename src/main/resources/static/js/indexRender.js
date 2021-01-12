
import * as THREE from './util/three.module.js';
import {STLLoader} from './util/STLLoader.js';

let degree = Math.PI / 180;

// Setup
let scene = new THREE.Scene();
let camera = new THREE.PerspectiveCamera(100, window.innerWidth / window.innerHeight, 0.1, 1000);
let renderer = new THREE.WebGLRenderer({antialias: true});
renderer.setPixelRatio(window.devicePixelRatio);
renderer.setSize(window.innerWidth / 2, window.innerHeight / 2);
renderer.outputEncoding = THREE.sRGBEncoding;
renderer.shadowMap.enabled = true;

//background fog kinda sets the color of the object
scene.background = new THREE.Color(0xadabab);
scene.fog = new THREE.Fog(0x111111, 0, 1500);
// Ground (comment out line: "scene.add( plane );" if Ground is not needed...)
let plane = new THREE.Mesh(
    new THREE.PlaneBufferGeometry(1000, 1000),
    new THREE.MeshPhongMaterial({color: 0x999999, specular: 0x101010})
);
plane.rotation.x = -90 * degree;
plane.position.y = -50;
plane.position.x = 0;
scene.add(plane);
plane.receiveShadow = true;

// Camera positioning
camera.position.z = 100;
camera.position.y = 50;
camera.rotation.x = degree;
let cameraTarget = new THREE.Vector3(0, -0.25, 0);

// Ambient light (necessary for Phong/Lambert-materials, not for Basic)
let ambientLight = new THREE.AmbientLight(0xffffff, 0.01);
scene.add(ambientLight);

scene.add(new THREE.HemisphereLight(0x443333, 0x111122));

function addShadowedLight(x, y, z, color, intensity) {
    const directionalLight = new THREE.DirectionalLight(color, intensity);
    directionalLight.position.set(x, y, z);
    scene.add(directionalLight);
    directionalLight.castShadow = true;
    const d = 1;
    directionalLight.shadow.camera.left = -d;
    directionalLight.shadow.camera.right = d;
    directionalLight.shadow.camera.top = d;
    directionalLight.shadow.camera.bottom = -d;
    directionalLight.shadow.camera.near = 1;
    directionalLight.shadow.camera.far = 4;
    directionalLight.shadow.bias = -0.002;
}

addShadowedLight(1, 1, 1, 0xffffff, 1.35);
addShadowedLight(0.5, 1, -1, 0xffaa00, 1);

let i;
for(i = 7;i < 13;i++) {
    var idString = "renderIndex" + i.toString();
    var fileUrl = $('#renderIndex' + i.toString()).data('original-title');
    if(!document.getElementById(idString)){continue;}
    let container = document.getElementById(idString)

    renderer.setSize($(container).width(), $(container).height());
    container.appendChild(renderer.domElement);


// Resize after viewport-size-change
    window.addEventListener('resize', onWindowResize, false);

    function onWindowResize() {

        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();

        renderer.setSize($(container).width(), $(container).height());
    }





// ASCII file - STL Import
    let loader = new STLLoader(); //loaders are for different files(ascii/binary), however you can't use them together bc it will render the file 2/3 times
// loader.load(fileUrl, function (geometry) {
//     let material = new THREE.MeshLambertMaterial({color: 0xFFFFFF, specular: 0x111111, shininess: 200});
//     let mesh = new THREE.Mesh(geometry, material);
//     mesh.position.set(0, 0, 0);
//     scene.add(mesh);
// });

// Binary files - STL Import
// loader.load(fileUrl, function (geometry) {
//     let material = new THREE.MeshLambertMaterial({color: 0xFFFFFF, specular: 0x111111, shininess: 200});
//     let mesh = new THREE.Mesh(geometry, material);
//     mesh.position.set(0, 20, 0);
//     scene.add(mesh);
// });

// Colored binary STL
    loader.load(fileUrl, function (geometry) {
        let meshMaterial = new THREE.MeshLambertMaterial({color: 0xFFFFFF, specular: 0x111111, shininess: 200});
        if (geometry.hasColors) {
            meshMaterial = new THREE.MeshPhongMaterial({opacity: geometry.alpha, vertexColors: true});
        }
        const mesh = new THREE.Mesh(geometry, meshMaterial);
        mesh.position.set(0, 0, 0);
        mesh.scale.set(0.3, 0.3, 0.3);
        mesh.castShadow = true;
        mesh.receiveShadow = true;
        scene.add(mesh);
    });

// Draw scene
    let render = function () {
        renderer.render(scene, camera);
        camera.lookAt(cameraTarget);
    };

// Run game loop (render,repeat)
    let GameLoop = function () {
        requestAnimationFrame(GameLoop,);

        render();
    };
    GameLoop();
}