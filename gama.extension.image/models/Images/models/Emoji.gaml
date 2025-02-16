/**
* Name: Emoji
* Shows how to use SVG versions of Emojis 
* Author: drogoul
* Tags: image, svg
*/


model Emojis

global {
	
	file emoji_folder <- folder("../includes/emoji/"); 
	// And filter the file names that end up with "svg"
	list<svg_file> file_list <- emoji_folder select (each contains (".svg")) collect (the_file: svg_file(emoji_folder.path + "/" + the_file));
	int size_max <- 8;
	init {
		int i <- 0;
		loop the_file over: 10 among(file_list) {
			i <- i + 1;
			geometry ss <- geometry(the_file);
			create emoji with: [ratio::ss.width / ss.height,shape::(geometry(the_file)),name::replace(replace(the_file.name,"emoji_",""),".svg",""), icon:: image(the_file)];
		}
	}
	
	
	
}

species emoji {
	float ratio;
	image icon;
	rgb color <- rnd_color(255);
	the_grid my_place <- the_grid(index);
	point location <- my_place.location;
	
	aspect icon_aspect {
		draw icon size: (ratio > 1) ? {size_max, size_max/ratio}: {size_max*ratio, size_max};
	}
	
	aspect shape_aspect {
		draw shape border: false color: color size: (ratio > 1) ? {size_max, size_max/ratio}: {size_max*ratio, size_max};
	}
}

grid the_grid width: 10 height: 10;

experiment Emojis {
	
	output {
		display "Show 100 vector emojis" type: 3d{
			species emoji aspect: shape_aspect;
		}
		display "Show 100 icon emojis" type: 3d {
			species emoji aspect: icon_aspect;
		}
	}
	
}

