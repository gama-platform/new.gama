/*******************************************************************************************************
 *
 * DefaultServerCommands.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * (v.2024-06).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.runtime.server;

import static gama.core.runtime.server.GamaServerMessage.Type.CommandExecutedSuccessfully;
import static gama.core.runtime.server.GamaServerMessage.Type.GamaServerError;
import static gama.core.runtime.server.GamaServerMessage.Type.MalformedRequest;
import static gama.core.runtime.server.GamaServerMessage.Type.UnableToExecuteRequest;
import static gama.core.runtime.server.ISocketCommand.ARGS;
import static gama.core.runtime.server.ISocketCommand.ESCAPED;
import static gama.core.runtime.server.ISocketCommand.EVALUATE;
import static gama.core.runtime.server.ISocketCommand.EXPR;
import static gama.core.runtime.server.ISocketCommand.NB_STEP;
import static gama.core.runtime.server.ISocketCommand.PARAMETERS;
import static gama.core.runtime.server.ISocketCommand.SYNC;
import static gama.core.runtime.server.ISocketCommand.SYNTAX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;

import gama.core.common.GamlFileExtension;
import gama.core.common.interfaces.IKeyword;
import gama.core.kernel.experiment.ExperimentAgent;
import gama.core.kernel.experiment.IExperimentPlan;
import gama.core.kernel.experiment.IParameter;
import gama.core.kernel.experiment.ITopLevelAgent;
import gama.core.kernel.model.IModel;
import gama.core.metamodel.agent.AgentReference;
import gama.core.metamodel.agent.IAgent;
import gama.core.runtime.ExecutionResult;
import gama.core.runtime.GAMA;
import gama.core.runtime.IScope;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.core.runtime.server.ISocketCommand.CommandException;
import gama.core.util.IList;
import gama.core.util.IMap;
import gama.core.util.file.json.Json;
import gama.core.util.file.json.JsonValue;
import gama.dev.DEBUG;
import gama.gaml.compilation.GAML;
import gama.gaml.compilation.GamaCompilationFailedException;
import gama.gaml.compilation.GamlCompilationError;
import gama.gaml.compilation.GamlIdiomsProvider;
import gama.gaml.operators.Cast;
import gama.gaml.species.ISpecies;
import gama.gaml.statements.ActionStatement;
import gama.gaml.statements.Arguments;
import gama.gaml.statements.IExecutable;
import gama.gaml.variables.IVariable;

/**
 * The Class DefaultServerCommands.
 *
 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
 * @date 15 oct. 2023
 */
public class DefaultServerCommands {

	/**
	 * Load.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage LOAD(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		final Object modelPath = map.get("model");
		final Object experiment = map.get("experiment");
		if (modelPath == null || experiment == null) return new CommandResponse(MalformedRequest,
				"For 'load', mandatory parameters are: 'model' and 'experiment'", map, false);
		String pathToModel = modelPath.toString().trim();
		String nameOfExperiment = experiment.toString().trim();
		File ff = new File(pathToModel);
		if (!ff.exists()) return new CommandResponse(UnableToExecuteRequest,
				"'" + ff.getAbsolutePath() + "' does not exist", map, false);
		if (!GamlFileExtension.isGaml(ff.getAbsoluteFile().toString()))
			return new CommandResponse(GamaServerMessage.Type.UnableToExecuteRequest,
					"'" + ff.getAbsolutePath() + "' is not a gaml file", map, false);
		IModel model = null;
		try {
			List<GamlCompilationError> errors = new ArrayList<>();
			model = GAML.getModelBuilder().compile(ff, errors, null);
		}
		catch (GamaCompilationFailedException compError) {
			return new CommandResponse(UnableToExecuteRequest, compError.toJsonString(), map, true);
		}
		catch (IOException e) {
			return new CommandResponse(UnableToExecuteRequest,
					"Impossible to compile '" + ff.getAbsolutePath() + "' because of " + e.getMessage(), map, false);
		}
		if (!model.getDescription().hasExperiment(nameOfExperiment)) return new CommandResponse(UnableToExecuteRequest,
				"'" + nameOfExperiment + "' is not an experiment present in '" + ff.getAbsolutePath() + "'", map,
				false);
		final IModel mm = model;
		GAMA.getGui().run("openExp", () -> GAMA.runGuiExperiment(nameOfExperiment, mm), false);
		return new CommandResponse(CommandExecutedSuccessfully, nameOfExperiment, map, false);
	}

	/**
	 * Play.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage PLAY(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		if (!GAMA.startFrontmostExperiment(true))
			return new CommandResponse(UnableToExecuteRequest, "Controller is full", map, false);
		return new CommandResponse(CommandExecutedSuccessfully, "", map, false);
	}

	/**
	 * Pause.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage PAUSE(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		IExperimentPlan plan;
		try {
			plan = server.retrieveExperimentPlan(socket, map);
		} catch (CommandException e) {
			return e.getResponse();
		}
		return new CommandResponse(
				plan.getController().processPause(true) ? CommandExecutedSuccessfully : UnableToExecuteRequest, "", map,
				false);
	}

	/**
	 * Step.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage STEP(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		IExperimentPlan plan;
		try {
			plan = server.retrieveExperimentPlan(socket, map);
		} catch (CommandException e) {
			return e.getResponse();
		}
		final int nb_step;
		if (map.get(NB_STEP) != null && !"".equals(("" + map.get(NB_STEP)).trim())) {
			nb_step = Integer.parseInt("" + map.get(NB_STEP));
		} else {
			nb_step = 1;
		}
		final boolean sync = map.get(SYNC) != null ? Boolean.parseBoolean("" + map.get(SYNC)) : false;
		for (int i = 0; i < nb_step; i++) {
			try {
				if (!plan.getController().processStep(sync))
					return new CommandResponse(UnableToExecuteRequest, "Controller is full", map, false);
			} catch (RuntimeException e) {
				DEBUG.OUT(e.getStackTrace());
				return new CommandResponse(GamaServerError, e, map, false);
			}
		}
		return new CommandResponse(CommandExecutedSuccessfully, "", map, false);
	}

	/**
	 * Back.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage BACK(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		IExperimentPlan plan;
		try {
			plan = server.retrieveExperimentPlan(socket, map);
		} catch (CommandException e) {
			return e.getResponse();
		}
		final int nb_step;
		if (map.get(NB_STEP) != null && !"".equals(("" + map.get(NB_STEP)).trim())) {
			nb_step = Integer.parseInt("" + map.get(NB_STEP));
		} else {
			nb_step = 1;
		}
		final boolean sync = map.get(SYNC) != null ? Boolean.parseBoolean("" + map.get(SYNC)) : false;
		for (int i = 0; i < nb_step; i++) {
			try {
				if (!plan.getController().processBack(sync))
					return new CommandResponse(UnableToExecuteRequest, "Controller is full", map, false);
			} catch (RuntimeException e) {
				DEBUG.OUT(e.getStackTrace());
				return new CommandResponse(GamaServerMessage.Type.GamaServerError, e, map, false);
			}
		}
		return new CommandResponse(CommandExecutedSuccessfully, "", map, false);
	}

	/**
	 * Stop.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage STOP(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		GAMA.closeAllExperiments(true, false);
		return new CommandResponse(CommandExecutedSuccessfully, "", map, false);
	}

	/**
	 * Reload.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage RELOAD(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		IExperimentPlan plan;
		try {
			plan = server.retrieveExperimentPlan(socket, map);
		} catch (CommandException e) {
			return e.getResponse();
		}
		IList params = (IList) map.get(PARAMETERS);
		// checking the parameters' format
		var parametersError = CommandExecutor.checkLoadParameters(params, map);
		if (parametersError != null) return parametersError;
		plan.setParameterValues(params);
		plan.setStopCondition((String) map.get(ISocketCommand.UNTIL));
		// actual reload
		if (!plan.getController().processReload(true))
			return new CommandResponse(UnableToExecuteRequest, "Controller is full", map, false);
		return new CommandResponse(CommandExecutedSuccessfully, "", map, false);
	}

	/**
	 * Eval.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage EVAL(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		IExperimentPlan plan;
		try {
			plan = server.retrieveExperimentPlan(socket, map);
		} catch (CommandException e) {
			return e.getResponse();
		}
		final Object expr = map.get(EXPR);
		if (expr == null) return new CommandResponse(MalformedRequest,
				"For " + EVALUATE + ", mandatory parameter is: " + EXPR, map, false);
		String entered = expr.toString().trim();
		String res = null;
		ITopLevelAgent agent = plan.getAgent();
		if (agent == null) { agent = GAMA.getPlatformAgent(); }
		final IScope scope = agent.getScope().copy("in web socket");
		if (entered.startsWith("?")) {
			res = GamlIdiomsProvider.getDocumentationOn(entered.substring(1));
		} else {
			try {
				final var expression = GAML.compileExpression(entered, agent, false);
				if (expression != null) { res = "" + scope.evaluate(expression, agent).getValue(); }
			} catch (final Exception e) {
				// error = true;
				res = "> Error: " + e.getMessage();
			} finally {
				agent.getSpecies().removeTemporaryAction();
				GAMA.releaseScope(scope);
			}
		}
		if (res == null || res.length() == 0 || res.startsWith("> Error: "))
			return new CommandResponse(UnableToExecuteRequest, res, map, false);
		final boolean escaped = map.get(ESCAPED) == null ? false : Boolean.parseBoolean("" + map.get(ESCAPED));
		return new CommandResponse(CommandExecutedSuccessfully, res, map, escaped);
	}

	/**
	 * Validate. Accepts
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param server
	 *            the server
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the gama server message
	 * @date 11 janv. 2024
	 */
	public static GamaServerMessage VALIDATE(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		final Object expr = map.get(EXPR);
		final Object syntax = map.get(SYNTAX);
		boolean syntaxOnly = syntax instanceof Boolean b && b;
		if (expr == null) return new CommandResponse(GamaServerMessage.Type.MalformedRequest,
				"For " + ISocketCommand.VALIDATE + ", mandatory parameter is: " + EXPR, map, false);
		String entered = expr.toString().trim();
		List<String> errors = GAML.validate(entered, syntaxOnly);
		if (errors != null && !errors.isEmpty()) return new CommandResponse(UnableToExecuteRequest, errors, map, false);
		final boolean escaped = map.get(ESCAPED) == null ? false : Boolean.parseBoolean("" + map.get(ESCAPED));
		return new CommandResponse(CommandExecutedSuccessfully, entered, map, escaped);
	}

	/**
	 * Ask.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the gama server message
	 * @date 26 nov. 2023
	 */
	public static GamaServerMessage ASK(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		IExperimentPlan plan;
		try {
			plan = server.retrieveExperimentPlan(socket, map);
		} catch (CommandException e) {
			return e.getResponse();
		}
		final String action = map.get(IKeyword.ACTION) != null ? map.get(IKeyword.ACTION).toString().trim() : null;
		if (action == null) return new CommandResponse(GamaServerMessage.Type.MalformedRequest,
				"For " + ISocketCommand.ASK + ", mandatory parameter is: 'action'", map, false);
		final String ref = map.get(IKeyword.AGENT) != null ? map.get(IKeyword.AGENT).toString().trim() : null;
		final ExperimentAgent exp = plan.getAgent();
		IScope scope = exp.getScope();
		final IAgent agent = ref == null ? exp : AgentReference.of(ref).getReferencedAgent(scope);
		if (agent == null)
			return new CommandResponse(UnableToExecuteRequest, "Agent does not exist: " + ref, map, false);
		final IExecutable exec = agent.getSpecies().getAction(action);
		if (exec == null) return new CommandResponse(UnableToExecuteRequest,
				"Action " + action + " does not exist in agent " + ref, map, false);
		// TODO Verify that it is not a JSON string...Otherwise, use Json.getNew().parse(...)
		String json = (String) map.get(ARGS);
		JsonValue object = Json.getNew().parse(json);
		Map<String, Object> args = Cast.asMap(scope, object.toGamlValue(scope), false);
		ExecutionResult er = ExecutionResult.PASSED;
		IScope newScope = agent.getScope().copy("Ask command of gama-server");
		try {
			er = newScope.execute(exec, agent, new Arguments(args));
		} catch (GamaRuntimeException e) {
			return new CommandResponse(UnableToExecuteRequest, e.getMessage(), map, false);
		} finally {
			GAMA.releaseScope(newScope);
		}
		if (!er.passed())
			return new CommandResponse(UnableToExecuteRequest, "Error in the execution of " + action, map, false);
		final boolean escaped = map.get(ISocketCommand.ESCAPED) == null ? false
				: Boolean.parseBoolean("" + map.get(ISocketCommand.ESCAPED));
		return new CommandResponse(CommandExecutedSuccessfully, "", map, escaped);
	}

	/**
	 * Download.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static CommandResponse DOWNLOAD(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		final String filepath = map.containsKey(IKeyword.FILE) ? map.get(IKeyword.FILE).toString() : null;
		if (filepath == null) return new CommandResponse(GamaServerMessage.Type.MalformedRequest,
				"For 'download', mandatory parameter is: 'file'", map, false);
		try (	InputStream is = Files.newInputStream(new File(filepath).toPath());
				InputStreamReader isr = new InputStreamReader(is, "UTF-8");
				BufferedReader br = new BufferedReader(isr)) {
			StringBuilder sc = new StringBuilder();
			String line;
			// read all the lines
			while ((line = br.readLine()) != null) { sc.append(line).append("\n"); }
			return new CommandResponse(CommandExecutedSuccessfully, sc.toString(), map, false);
		} catch (Exception e) {
			e.printStackTrace();
			return new CommandResponse(UnableToExecuteRequest, "Unable to download file", map, false);
		}
	}

	/**
	 * Upload.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage UPLOAD(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		final String filepath = map.containsKey("file") ? map.get("file").toString() : null;
		final String content = map.containsKey("content") ? map.get("content").toString() : null;
		if (filepath == null || content == null) return new CommandResponse(GamaServerMessage.Type.MalformedRequest,
				"For 'upload', mandatory parameters are: 'file' and 'content'", map, false);
		try (FileWriter myWriter = new FileWriter(filepath)) {
			myWriter.write(content);
			return new CommandResponse(GamaServerMessage.Type.CommandExecutedSuccessfully, "", map, false);
		} catch (Exception ex) {
			ex.printStackTrace();
			return new CommandResponse(GamaServerMessage.Type.UnableToExecuteRequest, ex.getMessage(), map, false);
		}
	}

	/**
	 * Description.
	 *
	 * @author Johary Rakotomalala (johary.rakotomalala.31@gmail.com)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the command response
	 * @date 24 jan. 2025
	 */
	public static GamaServerMessage DESCRIBE(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		// Check the parameters
		final Object modelPath = map.get("model");
		if (modelPath == null) return new CommandResponse(MalformedRequest,
				"For 'describe', mandatory parameter is: 'model'", map, false);
		String pathToModel = modelPath.toString().trim();
		File ff = new File(pathToModel);
		if (!ff.exists()) return new CommandResponse(UnableToExecuteRequest,
				"'" + ff.getAbsolutePath() + "' does not exist", map, false);
		if (!GamlFileExtension.isGaml(ff.getAbsoluteFile().toString()))
			return new CommandResponse(GamaServerMessage.Type.UnableToExecuteRequest,
					"'" + ff.getAbsolutePath() + "' is not a gaml file", map, false);
		IModel model = null;
		try {
			List<GamlCompilationError> errors = new ArrayList<>();
			model = GAML.getModelBuilder().compile(ff, errors, null);
		}
		catch (GamaCompilationFailedException compError) {
			return new CommandResponse(UnableToExecuteRequest, compError.toJsonString(), map, true);
		}
		catch (IOException e) {
			return new CommandResponse(UnableToExecuteRequest,
					"Impossible to compile '" + ff.getAbsolutePath() + "' because of " + e.getMessage(), map, false);
		}

		// get the parameters of what to include in the return message
		boolean readExperiments = (boolean) map.getOrDefault("experiments", true);
		boolean readSpeciesNames = (boolean) map.getOrDefault("speciesNames", true);
		boolean readspeciesVariables = (boolean) map.getOrDefault("speciesVariables", true);
		boolean readSpeciesActions = (boolean) map.getOrDefault("speciesActions", true) ;
		readSpeciesNames = readSpeciesNames || readSpeciesActions || readspeciesVariables; // if the variables or the actions of a species were asked we have to include the name
		
		// Gathering information
		Map<String, Object> res = new HashMap<String, Object>();
		
		if (readExperiments) {
			res.put("experiments", getExperiments(model));			
		}
		
		if (readSpeciesNames) {
			res.put("species", readSpecies(model, readSpeciesActions, readspeciesVariables));
		}

		res.put("name", model.getName());
		res.put("path", pathToModel);
		return new CommandResponse(CommandExecutedSuccessfully, res, map, false);
	}
	
	private static List<Map<String, String>> getSpeciesVariables(final ISpecies species){
		List<Map<String, String>> allVariables = new ArrayList<Map<String, String>>();		
		for (IVariable variable: species.getVars()) {
			Map<String, String> resVariable = new HashMap<String, String>();
			resVariable.put("name", variable.getName());
			resVariable.put("type", variable.getType().getName());
			allVariables.add(resVariable);
		}	
		return allVariables;
	}
	
	private static List<Map<String, Object>> getSpeciesActions(final ISpecies species) {
		List<Map<String, Object>> allActions = new ArrayList<Map<String, Object>>();		
		for (ActionStatement action : species.getActions()) {
			Map<String, Object> resAction = new HashMap<String, Object>();
			resAction.put("name", action.getName());
			List<Map<String, String>> resAllCommands  = new ArrayList<Map<String, String>>();
			var actionDescription = action.getDescription();
			for (var arg : actionDescription.getFormalArgs()) {
				Map<String, String> command = new HashMap<String, String>();
				command.put("name", arg.getName());
				command.put("type", arg.getGamlType().getName());
				resAllCommands.add(command);
			}
			
			resAction.put("parameters", resAllCommands);
			resAction.put("type", actionDescription.getGamlType().getName());
			allActions.add(resAction);
		}
		return allActions;
	}
	
	private static List<Map<String, Object>> readSpecies(IModel model, boolean readSpeciesActions, boolean readspeciesVariables) {
		List<Map<String, Object>> resAllSpecies = new ArrayList<Map<String, Object>>();		
		for (ISpecies species : model.getAllSpecies().values()) {
			// Name
			Map<String, Object> resSpecie = new HashMap<String, Object>();
			resSpecie.put("name", species.getName());
			
			// Variables
			if (readspeciesVariables) {			
				resSpecie.put("variables", getSpeciesVariables(species));
			}
			
			// Actions
			if (readSpeciesActions) {
				resSpecie.put("actions", getSpeciesActions(species));				
			}
			
			resAllSpecies.add(resSpecie);
		}
		return resAllSpecies;
	}

	private static List<Map<String, Object>> getExperiments(final IModel model) {
		List<Map<String, Object>> resAllExperiments = new ArrayList<Map<String, Object>>();
		// Get the experiments informations
		for (IExperimentPlan ittExp : model.getExperiments()) {
			// Get the parameters
			Map<String, Object> resExp = new HashMap<String, Object>();
			resExp.put("name", ittExp.getName());
			List<Map<String, String>> resAllParams = new ArrayList<Map<String,String>>();
			for (Map.Entry<String,IParameter> paramEntry : ittExp.getParameters().entrySet()) {
				Map<String, String> resParam = new HashMap<String, String>();
				IParameter param = paramEntry.getValue();
				resParam.put("name", param.getName());
				resParam.put("description", param.getTitle());
				resParam.put("type", param.getType().toString());
				resAllParams.add(resParam);
			}
			resExp.put("parameters", resAllParams);
			resAllExperiments.add(resExp);
		}
		return resAllExperiments;
	}

	/**
	 * Exit.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param socket
	 *            the socket
	 * @param map
	 *            the map
	 * @return the gama server message
	 * @date 15 oct. 2023
	 */
	public static GamaServerMessage EXIT(final GamaWebSocketServer server, final WebSocket socket,
			final IMap<String, Object> map) {
		try {
			return new CommandResponse(GamaServerMessage.Type.CommandExecutedSuccessfully, "", map, false);
		} finally {
			System.exit(0);
		}
	}

}
