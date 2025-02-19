/**
* Name: Loops
* Based on the internal empty template. 
* Author: baptiste
* Tags: 
*/

@no_warning
model Loops

species dummy {
	int value;
}

experiment loop_over type:test{
	
	test loop_over_list_int {
		let l <- [1,2,3,4];
		int sum <- 0;
		loop v over:l{
			sum <- sum + v;
		}
		assert sum = 10;
		
	}
	
	test loop_over_empty_list{
		loop v over:[] {
			assert false; // this should never happen as the list is empty
		}
		assert true;
	}
	
	test loop_over_species {
		int i <- 1;
		create dummy number:4{
			value <- i;
			i <- i + 1;
		}
		
		int sum <- 0;
		loop d over:dummy{
			sum <- sum + d.value;
		}
		assert sum = 10;
		ask dummy {
			do die;
		}
	}
	
	test loop_over_empty_species {
		int sum <- 0;
		loop d over: dummy{
			sum <- sum + d.value;
		}
		assert sum = 0;
	}
	
		
	// when we iterate over nil, nothing in the loop is executed and no exception is raised
	test loop_over_nil {
		bool exception_thrown <- false;
		bool code_executed <- true;
		try {
			loop i over:nil {
				assert false; // this code is not executed
			}	
			// if there's any code here, it should be executed
			code_executed <- true;	
		}
		catch {
			exception_thrown <- true;
		}
		assert not exception_thrown and code_executed;
	}
	
	test loop_over_list_int_break {
		let l <- [1,2,3,4];
		int sum <- 0;
		loop v over:l{
			sum <- sum + v;
			break;
		}
		assert sum = 1;
		
	}
	
}

experiment loop_times type:test{
	
	test loop_times {
		int i <- 1;
		int sum <- 0;
		loop times:4 {
			sum <- sum + i;
			i <- i + 1;
		}
		assert sum = 10;
	}
	
	
	test loop_zero_times {
		int i <- 1;
		int sum <- 0;
		loop times:0 {
			sum <- sum + i;
			i <- i + 1;
		}
		assert sum = 0;
	}
	
	test loop_negative_times {
		int i <- 1;
		int sum <- 0;
		loop times:-4 {
			sum <- sum + i;
			i <- i + 1;
		}
		assert sum = 0;
	}	

	
	// here the float is going to be truncated and not rounded
	test loop_float_times {
		int i <- 1;
		int sum <- 0;
		loop times:3.7 {
			sum <- sum + i;
			i <- i + 1;
		}
		assert sum = 6;
	}
	
	// when times is nil nothing in the loop is executed and no exception is risen
	test loop_nil_times {
		bool exception_thrown <- false;
		bool code_executed <- true;
		try {
			loop times:nil {
				assert false; // this code is not executed
			}		
			// if there's any code here, it should be executed
			code_executed <- true;	
		}
		catch {
			exception_thrown <- true;
		}
		assert not exception_thrown and code_executed;
	}
	
	test loop_string_times{
		bool exception_thrown <- false;
		bool code_executed <- true;
		try{
			loop times:"two"{
				assert false; // should never be executed
			}	
			// if there's any code here, it should be executed
			code_executed <- true;	
		}
		catch {
			exception_thrown <- true;
		}
		assert not exception_thrown and code_executed;
	}
	
	
	test loop_times_break {
		int i <- 1;
		int sum <- 0;
		loop times:4 {
			sum <- sum + i;
			i <- i + 1;
			break;
		}
		assert sum = 1;
	}
	
}

experiment loop_from_to type:test{
		

	test loop_from_to {
		int sum <- 0;
		loop i from:1 to: 4 {
			sum <- sum + i;
		}
		assert sum = 10;
	}
	
	test loop_from_negative_to_positive {
		int sum <- 0;
		loop i from:-1 to: 4 {
			sum <- sum + i;
		}
		assert sum = 9;
	}

	
	test loop_from_negative_to_negative {
		int sum <- 0;
		loop i from:-1 to: -4 {
			sum <- sum + i;
		}
		assert sum = -10;
	}
	
	
	test loop_to_from{
		int sum <-0;
		loop i from:4 to: 1 {
			sum <- sum + i;
		}
		assert sum = 10;		
	}

	test loop_from_to_step {
		int sum <- 0;
		loop i from:1 to: 4 step:1 {
			sum <- sum + i;
		}
		assert sum = 10;
	}
	
	
	test loop_from_to_step_2 {
		int sum <- 0;
		loop i from:1 to: 5 step:2 {
			sum <- sum + i;
		}
		assert sum = (1 + 3 + 5);
	}
	
	// In that case it should loop forever
	test loop_infinite_loop_negative_step_ints {
		bool success <- false;
		loop i from:1 to: 5 step:-1 {
			if i < -10 {// arbitrary number just to make sure we have made a few iterations already
				success <- true;
				break;
			}
		}
		assert success; // as it's infinite loop we should never reach this
	}
	
	// same as the previous one but looping in the other direction
	test loop_infinite_loop_positive_step_ints {
		bool success <- false;
		loop i from:5 to: 1 step:1 {
			if i > 10 {// arbitrary number just to make sure we have made a few iterations already
				success <- true;
				break;
			}
		}
		assert success; // as it's infinite loop we should never reach this
	}
	
	// same as the previous one but with floats
	test loop_infinite_loop_positive_step_floats {
		bool success <- false;
		loop i from:5.0 to: 1.0 step:1.0 {
			if i > 10 {// arbitrary number just to make sure we have made a few iterations already
				success <- true;
				break;
			}
		}

		assert success; // as it's infinite loop we should never reach this	
	}
	
	
	// same as the previous one but looping in the other direction
	test loop_infinite_loop_negative_step_floats {
		bool success <- false;
		loop i from:1.0 to: 5.0 step:-1.0 {
			if i < -10 {// arbitrary number just to make sure we have made a few iterations already
				success <- true;
				break;
			}
		}
		assert success; // as it's infinite loop we should never reach this	
	}
	
	
	// In the case of a floating step, the value of the loop will automatically 
	// become a float, even if from and to are integers
	test loop_integer_from_to_and_floating_step {
		float sum <- 0.0;
		loop i from:1 to: 5 step:1.6 {
			sum <- sum + i;
		}
		assert sum = 1.0 + 2.6 + 4.2;
	}
	
	test loop_floating_from_to{
		float sum <- 0.0;
		loop i from:1.0 to: 4.0 {
			sum <- sum + i;
		}
		assert sum = 10.0;		
	}
	
	test loop_floating_from_to_step{
		float sum <- 0.0;
		loop i from:0.0 to: 4.0 step:0.8 {
			sum <- sum + i;
		}
		assert sum = 0.8 + 1.6 + 2.4 + 3.2 + 4.0;		
	}
	
	test loop_floating_to_from {
		float sum <- 0.0;
		loop i from:4.0 to: 1.0 {
			sum <- sum + i;
		}
		assert sum = 10.0;		
	}
	
		
	test loop_floating_to_from_step {
		float sum <- 0.0;
		loop i from:4.0 to: 1.0 step:-2.0 {
			sum <- sum + i;
		}
		assert sum = 6.0;		
	}
	
	
		
	//break statements are able to get out of this loop
	test loop_from_to_break{
		float sum <- 0.0;
		loop i from:4.0 to: 1.0 step:-2.0 {
			sum <- sum + i;
			break;
		}
		assert sum = 4.0;	
	}
	
	
}


experiment loop_while type:test{
	
	test loop_while {
		int i <- 0;
		
		loop while:i < 10 {
			i <- i + 1;
		}
		assert i = 10;
	}
	
	// unlike do/while loops in some other programming languages, here if the condition is not met
	// since the beginning, we don't iterate even once.
	test loop_while_initially_wrong {
		int i <- 0;
		
		loop while:i = 10 {
			i <- i + 1;
		}
		assert i = 0;
	}
	
	// in case the condition is nil we don't iterate at all
	test loop_while_nil_condition {
		int i <- 0;
		
		loop while:nil {
			i <- i + 1;
		}
		assert i = 0;
	}
	
	// In case of a string condition too we do not iterate
	test loop_while_string_condition {
		int i <- 0;
		
		loop while:"rocambolesque" {
			i <- i + 1;
		}
		assert i = 0;
	}
	
	// In case of a list we iterate until it's empty
	test loop_while_empty_list {
		int i <- 0;
		
		loop while:[] {
			i <- i + 1;
		}
		assert i = 0;
	}
	
	// In case the list contains something we iterate 
	test loop_while_list {
		int i <- 0;
		list l <- [4,"banana",5, false, [], true];
		loop while:l {
			i <- i + 1;
			remove from:l index:0;
		}
		assert i = 6;
	}
	
	// a list containing only an empty list is still not empty itself so we should also iterate in that case
	test loop_while_list_of_empty_list {
		int i <- 0;
		list l <- [[]];
		loop while:l {
			i <- i + 1;
			remove from:l index:0;
		}
		assert i = 1;
	}
	
	//break statements are able to get out of this loop
	test loop_while_break{
		int i <- 0;
		
		loop while:i < 10 {
			i <- i + 1;
			break;
		}
		assert i = 1;
	}
	
}








