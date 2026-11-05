package es.iesmdc.profes;

import java.util.Arrays;
import java.util.List;

/** Accesos de la Zona Profesorado de iesmigueldecervantes.com (revisado el 05/11/2026). */
final class Defaults {
  private Defaults() {}

  static final String MINE = "Mis enlaces";

  static final String[] SECTIONS = {
      "Espacios web", "Gestión del centro", "Formación", "Legislación"
  };

  // Los identificadores no deben cambiar: se usan para guardar favoritos y ocultos.
  static final List<LinkItem> LINKS = Arrays.asList(
      d("sda", "Espacios web", "🧩", "Creador de SdA",
          "https://sites.google.com/murciaeduca.es/creadordesituacionesdeaprendiz/inicio"),
      d("actas", "Espacios web", "✍️", "Firma de actas", "http://firma.murciaeduca.es/"),
      d("aula", "Espacios web", "🎓", "Aula Virtual", "https://aulavirtual.murciaeduca.es/login/index.php"),
      d("intranet", "Espacios web", "📁", "Intranet Drive",
          "https://drive.google.com/drive/folders/1pqgkQKZsCsZAWvNuJIbW0mC33BMz1spF?usp=drive_link"),
      d("plumier", "Espacios web", "🏛️", "Plumier XXI", "https://profesores.murciaeduca.es/GICWeb/"),
      d("correo", "Espacios web", "✉️", "Correo web", "https://gmail.com/"),
      d("clip", "Espacios web", "🛠️", "Incidencias CLIP", "https://soporte.murciaeduca.es/"),
      d("firma", "Espacios web", "🔏", "Firma digital",
          "https://pase.carm.es/pase/login?service=https%3A%2F%2Fportafirmas.carm.es"),
      d("anota", "Espacios web", "📝", "Anota", "http://anota.murciaeduca.es"),

      d("faltas", "Gestión del centro", "📋", "Faltas profesorado",
          "https://main.iesmigueldecervantes.com/gestor-de-faltas-de-asistencia-del-profesorado"),
      d("rmi", "Gestión del centro", "💻", "Incidencias RMI", "https://forms.gle/vsNWKMLuFivEEew48"),
      d("fotocop", "Gestión del centro", "🖨️", "Alta fotocopiadora", "https://forms.gle/XUt9KptBGXWNKkuAA"),
      d("material", "Gestión del centro", "📦", "Material Secretaría",
          "https://main.iesmigueldecervantes.com/peticion-de-material-a-secretario-del-centro"),
      d("calendario", "Gestión del centro", "🗓️", "Calendario 26/27",
          "https://drive.google.com/file/d/179ck8AE7fhSKnWSplEMndQSq8d5RFCcb/view?usp=sharing"),
      d("lazarus", "Gestión del centro", "📱", "IMT Lazarus",
          "https://iesmigueldecervantes.imtlazarus.com/lazarus/access.php"),
      d("redar", "Gestión del centro", "📚", "REDAR", "https://main.iesmigueldecervantes.com/redar"),

      d("formacion", "Formación", "🧑‍🏫", "Formación profesorado",
          "https://sites.google.com/murciaeduca.es/mdcformaciontic/inicio"),
      d("fem", "Formación", "🏢", "Plataforma FEM",
          "https://educas.murciaeduca.es/cas/login?service=https%3A%2F%2Ffct.murciaeduca.es%2Ffctcarm%2Fshiro-cas#/home"),
      d("erasmus", "Formación", "🇪🇺", "Curso Erasmus+",
          "https://www.schooleducationgateway.eu/es/pub/index.htm"),

      d("orientacion", "Legislación", "🧭", "Orientación educativa",
          "https://www.carm.es/web/pagina?IDCONTENIDO=22298&IDTIPO=100&RASTRO=c148%24m"),
      d("legprof", "Legislación", "⚖️", "Leg. profesorado",
          "https://main.iesmigueldecervantes.com/legislacion-profesorado"),
      d("legedu", "Legislación", "📜", "Leg. educativa",
          "https://main.iesmigueldecervantes.com/legislacion-educativa")
  );

  /** Iconos sugeridos al crear un enlace propio. */
  static final String[] ICONS = {
      "🔗", "🌐", "📁", "📄", "📊", "📝", "🗓️", "✉️", "💬", "🎓", "📚", "🧪",
      "🧮", "🗺️", "🎨", "🎵", "⚽", "🧑‍🏫", "🏫", "🖥️", "📱", "🔒", "⭐", "📢",
      "🧾", "🗂️", "🎥", "🎧", "🧠", "🌍", "🔬", "📐"
  };

  private static LinkItem d(String id, String section, String icon, String title, String url) {
    return new LinkItem("d_" + id, section, icon, title, url, false);
  }
}
