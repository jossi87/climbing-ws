package com.buldreinfo.jersey.jaxb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.MetaHelper;
import com.buldreinfo.jersey.jaxb.helpers.Setup;
import com.buldreinfo.jersey.jaxb.helpers.Setup.GRADE_SYSTEM;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.Dangerous;
import com.buldreinfo.jersey.jaxb.model.Filter;
import com.buldreinfo.jersey.jaxb.model.FilterRequest;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Frontpage.RandomMedia;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaInfo;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemArea;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileMedia;
import com.buldreinfo.jersey.jaxb.model.ProfileStatistics;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.SitesRegion;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.model.UserSearch;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetHyperlink;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetWriter;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.buldreinfo.jersey.jaxb.xml.Webcam;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Api("/v2/")
@Path("/v2/")
public class V2 {
	private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private final static AuthHelper auth = new AuthHelper();
	private final static MetaHelper metaHelper = new MetaHelper();
	private static Logger logger = LogManager.getLogger();

	public V2() {
	}

	@ApiOperation(value = "Move media to trash")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@DELETE
	@Path("/media")
	public Response deleteMedia(@Context HttpServletRequest request,
			@ApiParam(value = "Media id", required = true) @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(id > 0);
			c.getBuldreinfoRepo().deleteMedia(authUserId, id);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get activity feed", response = Activity.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/activity")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getActivity(@Context HttpServletRequest request,
			@ApiParam(value = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@ApiParam(value = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector,
			@ApiParam(value = "Filter on lower grade", required = false) @QueryParam("lowerGrade") int lowerGrade,
			@ApiParam(value = "Include first ascents", required = false) @QueryParam("fa") boolean fa,
			@ApiParam(value = "Include comments", required = false) @QueryParam("comments") boolean comments,
			@ApiParam(value = "Include ticks (public ascents)", required = false) @QueryParam("ticks") boolean ticks,
			@ApiParam(value = "Include new media", required = false) @QueryParam("media") boolean media) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Activity> res = c.getBuldreinfoRepo().getActivity(authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get administrators", response = Administrator.class, responseContainer = "list")
	@GET
	@Path("/administrators")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getAdministrators(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			List<Administrator> administrators = c.getBuldreinfoRepo().getAdministrators(setup.getIdRegion());
			c.setSuccess();
			return Response.ok().entity(administrators).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get areas", response = Area.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getAreas(@Context HttpServletRequest request,
			@ApiParam(value = "Area id", required = false) @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Response response = null;
			if (id > 0) {
				try {
					Collection<Area> areas = Collections.singleton(c.getBuldreinfoRepo().getArea(setup, authUserId, id));
					response = Response.ok().entity(areas).build();
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
					Redirect res = c.getBuldreinfoRepo().getCanonicalUrl(id, 0, 0);
					response = Response.ok().entity(res).build();
				}
			}
			else {
				Collection<Area> areas = c.getBuldreinfoRepo().getAreaList(authUserId, setup.getIdRegion());
				response = Response.ok().entity(areas).build();
			}
			c.setSuccess();
			return response;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get area PDF by id", response = Byte[].class)
	@GET
	@Path("/areas/pdf")
	@Produces("application/pdf")
	public Response getAreasPdf(@Context final HttpServletRequest request,
			@ApiParam(value = "Access token", required = false) @QueryParam("accessToken") String accessToken,
			@ApiParam(value = "Area id", required = true) @QueryParam("id") int id) throws Throwable{
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, metaHelper, accessToken);
			final Meta meta = new Meta(c, setup, authUserId);
			final Area area = c.getBuldreinfoRepo().getArea(setup, authUserId, id);
			final Collection<GradeDistribution> gradeDistribution = c.getBuldreinfoRepo().getGradeDistribution(authUserId, setup, area.getId(), 0);
			final List<Sector> sectors = new ArrayList<>();
			final boolean orderByGrade = false;
			for (Area.Sector sector : area.getSectors()) {
				Sector s = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, sector.getId());
				sectors.add(s);
			}
			c.setSuccess();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeArea(meta, area, gradeDistribution, sectors);
						}
					} catch (Throwable e) {
						e.printStackTrace();
						throw GlobalFunctions.getWebApplicationExceptionInternalError(new Exception(e.getMessage()));
					}	            	 
				}
			};
			String fn = GlobalFunctions.getFilename(area.getName(), "pdf");
			return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"" + fn + "\"" ).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get webcams", response = Webcam.class, responseContainer = "list")
	@GET
	@Path("/webcams")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getCameras(@Context HttpServletRequest request) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			VegvesenParser vegvesenPaser = new VegvesenParser();
			List<Webcam> res = vegvesenPaser.getCameras();
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get boulders/routes marked as dangerous", response = Dangerous.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/dangerous")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getDangerous(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Collection<Dangerous> res = c.getBuldreinfoRepo().getDangerous(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get frontpage", response = Frontpage.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/frontpage")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getFrontpage(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Frontpage res = c.getBuldreinfoRepo().getFrontpage(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get grade distribution by Area Id or Sector Id", response = GradeDistribution.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/grade/distribution")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getGradeDistribution(@Context HttpServletRequest request,
			@ApiParam(value = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@ApiParam(value = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Collection<GradeDistribution> res = c.getBuldreinfoRepo().getGradeDistribution(authUserId, setup, idArea, idSector);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get graph (number of boulders/routes grouped by grade)", response = GradeDistribution.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/graph")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getGraph(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Collection<GradeDistribution> res = c.getBuldreinfoRepo().getContentGraph(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	/**
	 * crc32 is included to ensure correct version downloaded, and not old version from browser cache (e.g. if rotated image)
	 */
	@ApiOperation(value = "Get media by id", response = Byte[].class, produces = "image/*")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/images")
	public Response getImages(@Context HttpServletRequest request,
			@ApiParam(value = "Media id", required = true) @QueryParam("id") int id,
			@ApiParam(value = "Checksum - not used in ws, but necessary to include on client when an image is changed (e.g. rotated) to avoid cached version", required = false) @QueryParam("crc32") int crc32,
			@ApiParam(value = "Image size - E.g. minDimention=100 can return an image with the size 100x133px", required = false) @QueryParam("minDimention") int minDimention) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Point dimention = minDimention == 0? null : c.getBuldreinfoRepo().getMediaDimention(id);
			final String acceptHeader = request.getHeader("Accept");
			final boolean webP = dimention == null && acceptHeader != null && acceptHeader.contains("image/webp");
			final String mimeType = webP? "image/webp" : "image/jpeg";
			final java.nio.file.Path p = c.getBuldreinfoRepo().getImage(webP, id);
			c.setSuccess();
			CacheControl cc = new CacheControl();
			cc.setMaxAge(2678400); // 31 days
			cc.setNoTransform(false);
			if (dimention != null) {
				BufferedImage b = Preconditions.checkNotNull(ImageIO.read(p.toFile()), "Could not read " + p.toString());
				Mode mode = dimention.getX() < dimention.getY()? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
				BufferedImage scaled = Scalr.resize(b, mode, minDimention);
				b.flush();
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(scaled, "jpg", baos);
					byte[] imageData = baos.toByteArray();
					return Response.ok(imageData, mimeType).cacheControl(cc).build();
				}
			}
			return Response.ok(p.toFile(), mimeType).cacheControl(cc).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get Media by id", response = Media.class)
	@GET
	@Path("/media")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getMedia(@Context HttpServletRequest request,
			@ApiParam(value = "Media id", required = true) @QueryParam("idMedia") int idMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Media res = c.getBuldreinfoRepo().getMedia(idMedia);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get metadata", response = Meta.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getMeta(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Meta res = new Meta(c, setup, authUserId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get permissions", response = PermissionUser.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getPermissions(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<PermissionUser> res = c.getBuldreinfoRepo().getPermissions(authUserId, setup.getIdRegion());
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get problem by id", response = Problem.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/problem")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblem(@Context HttpServletRequest request,
			@ApiParam(value = "Problem id", required = true) @QueryParam("id") int id,
			@ApiParam(value = "Include hidden media (example: if a sector has multiple topo-images, the topo-images without this route will be hidden)", required = false) @QueryParam("showHiddenMedia") boolean showHiddenMedia
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Response response = null;
			try {
				Problem res = c.getBuldreinfoRepo().getProblem(authUserId, setup, id, showHiddenMedia);
				response = Response.ok().entity(res).build();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
				Redirect res = c.getBuldreinfoRepo().getCanonicalUrl(0, 0, id);
				response = Response.ok().entity(res).build();
			}
			c.setSuccess();
			return response;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get problem PDF by id", response = Byte[].class)
	@GET
	@Path("/problem/pdf")
	@Produces("application/pdf")
	public Response getProblemPdf(@Context final HttpServletRequest request,
			@ApiParam(value = "Access token", required = false) @QueryParam("accessToken") String accessToken,
			@ApiParam(value = "Problem id", required = true) @QueryParam("id") int id) throws Throwable{
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, metaHelper, accessToken);
			final Problem problem = c.getBuldreinfoRepo().getProblem(authUserId, setup, id, false);
			final Area area = c.getBuldreinfoRepo().getArea(setup, authUserId, problem.getAreaId());
			final Sector sector = c.getBuldreinfoRepo().getSector(authUserId, false, setup, problem.getSectorId());
			c.setSuccess();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeProblem(area, sector, problem);
						}
					} catch (Throwable e) {
						e.printStackTrace();
						throw GlobalFunctions.getWebApplicationExceptionInternalError(new Exception(e.getMessage()));
					}	            	 
				}
			};
			String fn = GlobalFunctions.getFilename(problem.getName(), "pdf");
			return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"" + fn + "\"" ).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get problems", response = ProblemArea.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblems(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<ProblemArea> res = c.getBuldreinfoRepo().getProblemsList(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get problems as Excel (xlsx)", response = Byte[].class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/problems/xlsx")
	@Produces(MIME_TYPE_XLSX)
	public Response getProblemsXlsx(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<ProblemArea> res = c.getBuldreinfoRepo().getProblemsList(authUserId, setup);
			byte[] bytes;
			try (ExcelReport report = new ExcelReport()) {
				try (SheetWriter writer = report.addSheet("TOC")) {
					for (ProblemArea a : res) {
						for (ProblemArea.Sector s : a.getSectors()) {
							for (ProblemArea.Problem p : s.getProblems()) {
								writer.incrementRow();
								writer.write("URL", SheetHyperlink.of(p.getUrl()));
								writer.write("AREA", a.getName());
								writer.write("SECTOR", s.getName());
								writer.write("NR", p.getNr());
								writer.write("NAME", p.getName());
								writer.write("GRADE", p.getGrade());
								String type = p.getT().getType();
								if (p.getT().getSubType() != null) {
									type += " (" + p.getT().getSubType() + ")";			
								}
								writer.write("TYPE", type);
								if (!setup.isBouldering()) {
									writer.write("PITCHES", p.getNumPitches() > 0? p.getNumPitches() : 1);
								}
								writer.write("FA", p.getFa());
								writer.write("STARS", p.getStars());
								writer.write("DESCRIPTION", p.getDescription());
							}
						}
					}
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					report.writeExcel(os);
					bytes = os.toByteArray();
				}
			}
			c.setSuccess();
			String fn = GlobalFunctions.getFilename("ProblemsList", "xlsx");
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Disposition", "attachment; filename=\"" + fn + "\"" )
					.build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get profile by id", response = Profile.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/profile")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProfile(@Context HttpServletRequest request,
			@ApiParam(value = "User id (will return logged in user without this attribute)", required = true) @QueryParam("id") int reqUserId) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Profile res = c.getBuldreinfoRepo().getProfile(authUserId, setup, reqUserId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get profile media by id", response = ProfileMedia.class, responseContainer = "list")
	@GET
	@Path("/profile/media")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProfilemedia(@Context HttpServletRequest request,
			@ApiParam(value = "User id", required = true) @QueryParam("id") int id,
			@ApiParam(value = "FALSE = tagged media, TRUE = captured media", required = false) @QueryParam("captured") boolean captured
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<ProfileMedia> res = c.getBuldreinfoRepo().getProfileMediaProblem(authUserId, setup, id, captured);
			if (captured) {
				res.addAll(c.getBuldreinfoRepo().getProfileMediaCapturedSector(authUserId, setup, id));
				res.sort(Comparator.comparingInt(ProfileMedia::getId).reversed());
			}
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get profile statistics by id", response = ProfileStatistics.class)
	@GET
	@Path("/profile/statistics")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProfileStatistics(@Context HttpServletRequest request,
			@ApiParam(value = "User id", required = true) @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			ProfileStatistics res = c.getBuldreinfoRepo().getProfileStatistics(authUserId, setup, id);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get profile todo", response = ProfileTodo.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/profile/todo")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProfileTodo(@Context HttpServletRequest request,
			@ApiParam(value = "User id", required = true) @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			ProfileTodo res = c.getBuldreinfoRepo().getProfileTodo(authUserId, setup, id);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get robots.txt", response = String.class)
	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
	public Response getRobotsTxt(@Context HttpServletRequest request) {
		final Setup setup = metaHelper.getSetup(request);
		if (setup.isSetRobotsDenyAll()) {
			return Response.ok().entity("User-agent: *\r\nDisallow: /").build(); 
		}
		List<String> lines = Lists.newArrayList(
				"User-agent: *",
				"Disallow: */pdf", // Disallow all pdf-calls
				"Sitemap: " + setup.getUrl("/sitemap.txt"));
		return Response.ok().entity(Joiner.on("\r\n").join(lines)).build(); 
	}

	@ApiOperation(value = "Get sector by id", response = Sector.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getSectors(@Context HttpServletRequest request,
			@ApiParam(value = "Sector id", required = true) @QueryParam("id") int id
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			final boolean orderByGrade = setup.isBouldering();
			Response response = null;
			try {
				Sector s = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, id);
				response = Response.ok().entity(s).build();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
				Redirect res = c.getBuldreinfoRepo().getCanonicalUrl(0, id, 0);
				response = Response.ok().entity(res).build();
			}
			c.setSuccess();
			return response;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get sector PDF by id", response = Byte[].class)
	@GET
	@Path("/sectors/pdf")
	@Produces("application/pdf")
	public Response getSectorsPdf(@Context final HttpServletRequest request,
			@ApiParam(value = "Access token", required = false) @QueryParam("accessToken") String accessToken,
			@ApiParam(value = "Sector id", required = true) @QueryParam("id") int id) throws Throwable{
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, metaHelper, accessToken);
			final Meta meta = new Meta(c, setup, authUserId);
			final Sector sector = c.getBuldreinfoRepo().getSector(authUserId, false, setup, id);
			final Collection<GradeDistribution> gradeDistribution = c.getBuldreinfoRepo().getGradeDistribution(authUserId, setup, 0, id);
			final Area area = c.getBuldreinfoRepo().getArea(setup, authUserId, sector.getAreaId());
			c.setSuccess();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeArea(meta, area, gradeDistribution, Lists.newArrayList(sector));
						}
					} catch (Throwable e) {
						e.printStackTrace();
						throw GlobalFunctions.getWebApplicationExceptionInternalError(new Exception(e.getMessage()));
					}	            	 
				}
			};
			String fn = GlobalFunctions.getFilename(sector.getName(), "pdf");
			return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"" + fn + "\"" ).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get sitemap.txt", response = String.class)
	@GET
	@Path("/sitemap.txt")
	@Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
	public Response getSitemapTxt(@Context HttpServletRequest request, @QueryParam("base") String base) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			String res = c.getBuldreinfoRepo().getSitemapTxt(setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get sites", response = SitesRegion.class, responseContainer = "list")
	@GET
	@Path("/sites")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getSites(@Context HttpServletRequest request,
			@ApiParam(value = "Type (BOULDER/CLIMBING/ICE)", required = true) @QueryParam("type") String type
			) throws ExecutionException, IOException {
		GRADE_SYSTEM system = null;
		switch (Strings.nullToEmpty(type).toUpperCase()) {
		case "BOULDER": system = GRADE_SYSTEM.BOULDER; break;
		case "CLIMBING": system = GRADE_SYSTEM.CLIMBING; break;
		case "ICE": system = GRADE_SYSTEM.ICE; break;
		default: throw new RuntimeException("Invalid type=" + type);
		}
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			List<SitesRegion> res = c.getBuldreinfoRepo().getSites(setup.getIdRegion(), system);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get ticks (public ascents)", response = Ticks.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTicks(@Context HttpServletRequest request,
			@ApiParam(value = "Page (ticks ordered descending, 0 returns fist page)", required = false) @QueryParam("page") int page
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Ticks res = c.getBuldreinfoRepo().getTicks(authUserId, setup, page);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get todo on Area/Sector", response = Todo.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTodo(@Context HttpServletRequest request,
			@ApiParam(value = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@ApiParam(value = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Todo res = c.getBuldreinfoRepo().getTodo(authUserId, setup, idArea, idSector);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get top on Area/Sector", response = Top.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/top")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTop(@Context HttpServletRequest request, 
			@ApiParam(value = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@ApiParam(value = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			Collection<Top> res = c.getBuldreinfoRepo().getTop(setup, idArea, idSector);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get trash", response = Trash.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@GET
	@Path("/trash")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTrash(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Trash> res = c.getBuldreinfoRepo().getTrash(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Search for user", response = UserSearch.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@GET
	@Path("/users/search")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsersSearch(@Context HttpServletRequest request,
			@ApiParam(value = "Search keyword", required = true) @QueryParam("value") String value
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			List<UserSearch> res = c.getBuldreinfoRepo().getUserSearch(authUserId, value);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", response = Byte[].class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@GET
	@Path("/users/ticks")
	@Produces(MIME_TYPE_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(authUserId>0, "User not logged in");
			byte[] bytes = c.getBuldreinfoRepo().getUserTicks(authUserId);
			c.setSuccess();

			String fn = GlobalFunctions.getFilename("Ticks", "xlsx");
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Disposition", "attachment; filename=\"" + fn + "\"" )
					.build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get Frontpage without JavaScript (for embedding on e.g. Facebook)", response = String.class)
	@GET
	@Path("/without-js")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getWithoutJs(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = 0;
			Frontpage f = c.getBuldreinfoRepo().getFrontpage(authUserId, setup);
			String description = String.format("%s - %d %s, %d public ascents, %d images, %d ascents on video",
					setup.getDescription(),
					f.getNumProblems(),
					(setup.isBouldering()? "boulders" : "routes"),
					f.getNumTicks(),
					f.getNumImages(),
					f.getNumMovies());
			RandomMedia randomMedia = f.getRandomMedia();
			String html = getHtml(setup,
					setup.getUrl(),
					setup.getTitle(),
					description,
					(randomMedia == null? 0 : randomMedia.getIdMedia()),
					(randomMedia == null? 0 : randomMedia.getWidth()),
					(randomMedia == null? 0 : randomMedia.getHeight()));
			c.setSuccess();
			return Response.ok().entity(html).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get area by id without JavaScript (for embedding on e.g. Facebook)", response = String.class)
	@GET
	@Path("/without-js/area/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getWithoutJsArea(@Context HttpServletRequest request, @ApiParam(value = "Area id", required = true) @PathParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = 0;
			Area a = c.getBuldreinfoRepo().getArea(setup, authUserId, id);
			String description = null;
			String info = a.getTypeNumTicked() == null || a.getTypeNumTicked().isEmpty()? null : a.getTypeNumTicked()
					.stream()
					.map(tnt -> tnt.getNum() + " " + tnt.getType().toLowerCase())
					.collect(Collectors.joining(", "));
			if (setup.isBouldering()) {
				description = String.format("Bouldering in %s (%s)", a.getName(), info);
			}
			else {
				description = String.format("Climbing in %s (%s)", a.getName(), info);
			}
			Media m = a.getMedia() != null && !a.getMedia().isEmpty()? a.getMedia().get(0) : null;
			String html = getHtml(setup,
					setup.getUrl("/area/" + a.getId()),
					a.getName(),
					description,
					(m == null? 0 : m.getId()),
					(m == null? 0 : m.getWidth()),
					(m == null? 0 : m.getHeight()));
			c.setSuccess();
			return Response.ok().entity(html).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get problem by id without JavaScript (for embedding on e.g. Facebook)", response = String.class)
	@GET
	@Path("/without-js/problem/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getWithoutJsProblem(@Context HttpServletRequest request, @ApiParam(value = "Problem id", required = true) @PathParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = 0;
			Problem p = c.getBuldreinfoRepo().getProblem(authUserId, setup, id, false);
			String title = String.format("%s [%s] (%s / %s)", p.getName(), p.getGrade(), p.getAreaName(), p.getSectorName());
			String description = p.getComment();
			if (p.getFa() != null && !p.getFa().isEmpty()) {
				String fa = Joiner.on(", ").join(p.getFa().stream().map(x -> x.getName().trim()).collect(Collectors.toList()));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.getFaDateHr())? " (" + p.getFaDate() + ")" : "");
			}
			Media m = null;
			if (p.getMedia() != null && !p.getMedia().isEmpty()) {
				Optional<Media> optM = p.getMedia().stream().filter(x -> !x.isInherited()).findFirst();
				if (optM.isPresent()) {
					m = optM.get();
				}
				else {
					m = p.getMedia().get(0);
				}
			}
			String html = getHtml(setup,
					setup.getUrl("/problem/" + p.getId()),
					title,
					description,
					(m == null? 0 : m.getId()),
					(m == null? 0 : m.getWidth()),
					(m == null? 0 : m.getHeight()));
			c.setSuccess();
			return Response.ok().entity(html).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Get sector by id without JavaScript (for embedding on e.g. Facebook)", response = String.class)
	@GET
	@Path("/without-js/sector/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getWithoutJsSector(@Context HttpServletRequest request, @ApiParam(value = "Sector id", required = true) @PathParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = 0;
			final boolean orderByGrade = false;
			Sector s = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, id);
			String title = String.format("%s (%s)", s.getName(), s.getAreaName());
			String description = String.format("%s in %s / %s (%d %s)%s",
					(setup.isBouldering()? "Bouldering" : "Climbing"),
					s.getAreaName(),
					s.getName(),
					(s.getProblems() != null? s.getProblems().size() : 0),
					(setup.isBouldering()? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.getComment())? " | " + s.getComment() : ""));
			Media m = s.getMedia() != null && !s.getMedia().isEmpty()? s.getMedia().get(0) : null;
			String html = getHtml(setup,
					setup.getUrl("/sector/" + s.getId()),
					title,
					description,
					(m == null? 0 : m.getId()),
					(m == null? 0 : m.getWidth()),
					(m == null? 0 : m.getHeight()));
			c.setSuccess();
			return Response.ok().entity(html).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update area (area must be provided as json on field \"json\" in multiPart)", response = Redirect.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/areas")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postAreas(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Area a = new Gson().fromJson(multiPart.getField("json").getValue(), Area.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			Redirect res = c.getBuldreinfoRepo().setArea(setup, authUserId, a, multiPart);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update comment (comment must be provided as json on field \"json\" in multiPart)")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/comments")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postComments(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Comment co = new Gson().fromJson(multiPart.getField("json").getValue(), Comment.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			final Setup setup = metaHelper.getSetup(request);
			c.getBuldreinfoRepo().upsertComment(authUserId, setup, co, multiPart);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Filter on boulders/routes", response = Filter.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@POST
	@Path("/filter")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postFilter(@Context HttpServletRequest request, FilterRequest fr) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Filter> res = c.getBuldreinfoRepo().getFilter(authUserId, setup, fr);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update Media SVG")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/media/svg")
	public Response postMediaSvg(@Context HttpServletRequest request, Media m) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().upsertMediaSvg(authUserId, setup, m);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update user privilegies")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().upsertPermissionUser(setup.getIdRegion(), authUserId, u);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update problem (problem must be provided as json on field \"json\" in multiPart)", response = Redirect.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/problems")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postProblems(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			Redirect res = c.getBuldreinfoRepo().setProblem(authUserId, setup, p, multiPart);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Add media on problem (problem must be provided as json on field \"json\" in multiPart)", response = Problem.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/problems/media")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postProblemsMedia(@Context HttpServletRequest request,
			@ApiParam(value = "Problem id", required = true) @QueryParam("problemId") int problemId,
			FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(p.getId() > 0);
			Preconditions.checkArgument(!p.getNewMedia().isEmpty());
			c.getBuldreinfoRepo().addProblemMedia(authUserId, p, multiPart);
			c.setSuccess();
			Problem res = c.getBuldreinfoRepo().getProblem(authUserId, setup, p.getId(), false);
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update topo line on route/boulder (SVG on sector/problem-image)")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/problems/svg")
	public Response postProblemsSvg(@Context HttpServletRequest request,
			@ApiParam(value = "Problem id", required = true) @QueryParam("problemId") int problemId,
			@ApiParam(value = "Media id", required = true) @QueryParam("mediaId") int mediaId,
			Svg svg
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Preconditions.checkNotNull(svg, "Invalid svg=" + svg);
			c.getBuldreinfoRepo().upsertSvg(authUserId, problemId, mediaId, svg);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Search for area/sector/problem/user", response = Search.class, responseContainer = "list")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = false, dataType = "string", paramType = "header") })
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			String search = Strings.emptyToNull(Strings.nullToEmpty(sr.getValue()).trim());
			Preconditions.checkNotNull(search, "Invalid search: " + search);
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Search> res = c.getBuldreinfoRepo().getSearch(authUserId, setup, search);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update sector (sector smust be provided as json on field \"json\" in multiPart)", response = Redirect.class)
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/sectors")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postSectors(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Sector s = new Gson().fromJson(multiPart.getField("json").getValue(), Sector.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			final boolean orderByGrade = setup.isBouldering();
			Redirect res = c.getBuldreinfoRepo().setSector(authUserId, orderByGrade, setup, s, multiPart);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update tick (public ascent)")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(t.getIdProblem() > 0);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setTick(authUserId, setup, t);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update todo")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/todo")
	@Consumes(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postTodo(@Context HttpServletRequest request,
			@ApiParam(value = "Problem id", required = true) @QueryParam("idProblem") int idProblem
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().toggleTodo(authUserId, idProblem);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update visible regions")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@POST
	@Path("/user/regions")
	public Response postUserRegions(@Context HttpServletRequest request,
			@ApiParam(value = "Region id", required = true) @QueryParam("regionId") int regionId,
			@ApiParam(value = "Delete (TRUE=hide, FALSE=show)", required = true) @QueryParam("delete") boolean delete
			) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setUserRegion(authUserId, regionId, delete);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update media location")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@PUT
	@Path("/media")
	public Response putMedia(@Context HttpServletRequest request,
			@ApiParam(value = "Move right", required = true) @QueryParam("id") int id,
			@ApiParam(value = "Move left", required = true) @QueryParam("left") boolean left,
			@ApiParam(value = "To sector id (will move media to sector if toSectorId>0 and toProblemId=0)", required = true) @QueryParam("toIdSector") int toIdSector,
			@ApiParam(value = "To problem id (will move media to problem if toProblemId>0 and toSectorId=0)", required = true) @QueryParam("toIdProblem") int toIdProblem
			) throws ExecutionException, IOException {
		Preconditions.checkArgument((left && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdSector > 0 && toIdProblem == 0) ||
				(!left && toIdSector == 0 && toIdProblem > 0),
				"Invalid arguments");
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(id > 0);
			c.getBuldreinfoRepo().moveMedia(authUserId, id, left, toIdSector, toIdProblem);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update media info")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@PUT
	@Path("/media/info")
	public Response putMediaInfo(@Context HttpServletRequest request, MediaInfo m) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(authUserId > 0);
			c.getBuldreinfoRepo().updateMediaInfo(authUserId, m);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Update media rotation")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@PUT
	@Path("/media/jpeg/rotate")
	public Response putMediaJpegRotate(@Context HttpServletRequest request,
			@ApiParam(value = "Media id", required = true) @QueryParam("idMedia") int idMedia,
			@ApiParam(value = "Degrees (90/180/270)", required = true) @QueryParam("degrees") int degrees
			) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().rotateMedia(setup.getIdRegion(), authUserId, idMedia, degrees);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@ApiOperation(value = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)")
	@ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header") })
	@PUT
	@Path("/trash")
	public Response putTrash(@Context HttpServletRequest request,
			@ApiParam(value = "Area id", required = true) @QueryParam("idArea") int idArea,
			@ApiParam(value = "Sector id", required = true) @QueryParam("idSector") int idSector,
			@ApiParam(value = "Problem id", required = true) @QueryParam("idProblem") int idProblem,
			@ApiParam(value = "Media id", required = true) @QueryParam("idMedia") int idMedia
			) throws ExecutionException, IOException {
		Preconditions.checkArgument(
				(idArea > 0 && idSector == 0 && idProblem == 0) ||
				(idArea == 0 && idSector > 0 && idProblem == 0) ||
				(idArea == 0 && idSector == 0 && idProblem > 0) ||
				(idArea == 0 && idSector == 0 && idProblem == 0),
				"Invalid arguments");
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().trashRecover(setup, authUserId, idArea, idSector, idProblem, idMedia);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	private String getHtml(Setup setup, String url, String title, String description, int mediaId, int mediaWidth, int mediaHeight) {
		String ogImage = "";
		if (mediaId > 0) {
			String image = setup.getUrl("/buldreinfo_media/jpg/" + String.valueOf(mediaId/100*100) + "/" + mediaId + ".jpg");
			ogImage = "<meta property=\"og:image\" content=\"" + image + "\" />" + 
					"<meta property=\"og:image:width\" content=\"" + mediaWidth + "\" />" + 
					"<meta property=\"og:image:height\" content=\"" + mediaHeight + "\" />";
		}
		String html = "<html><head>" +
				"<meta charset=\"UTF-8\">" +
				"<title>" + title + "</title>" + 
				"<meta name=\"description\" content=\"" + description + "\" />" + 
				"<meta property=\"og:type\" content=\"website\" />" + 
				"<meta property=\"og:description\" content=\"" + description + "\" />" + 
				"<meta property=\"og:url\" content=\"" + url + "\" />" + 
				"<meta property=\"og:title\" content=\"" + title + "\" />" + 
				"<meta property=\"fb:app_id\" content=\"275320366630912\" />" +
				ogImage +
				"</head></html>";
		return html;
	}
	
	private int getUserId(HttpServletRequest request) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = auth.getUserId(c, request, metaHelper);
			c.setSuccess();
			return authUserId;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}